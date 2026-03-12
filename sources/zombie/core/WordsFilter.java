/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public class WordsFilter {
    private final int universalCharactersMax = 3;
    private static WordsFilter instance;
    private static final HashSet<Character> SKIP_SYMBOL;
    private static final HashSet<Character> UNIVERSAL_SYMBOL;
    private static final Map<Character, List<Character>> SYMBOL_REPLACEMENTS;
    private TreeNode root = new TreeNode(this, ' ');

    public static WordsFilter getInstance() {
        if (instance == null) {
            instance = new WordsFilter();
        }
        return instance;
    }

    public void loadWords(String badWordsFilename, String goodWordsFilename) {
        String line;
        BufferedReader reader;
        if (goodWordsFilename != null && !goodWordsFilename.isEmpty()) {
            try {
                reader = new BufferedReader(new FileReader(goodWordsFilename));
                try {
                    while ((line = reader.readLine()) != null) {
                        this.insertWord(line.trim().toLowerCase(), WordType.Good);
                    }
                }
                finally {
                    reader.close();
                }
            }
            catch (FileNotFoundException e) {
                DebugLog.General.error("Can't open file with good words (" + goodWordsFilename + ")");
            }
            catch (IOException e) {
                DebugLog.General.printException(e, "Can't load file with good words", LogSeverity.Error);
            }
        }
        if (badWordsFilename != null && !badWordsFilename.isEmpty()) {
            try {
                reader = new BufferedReader(new FileReader(badWordsFilename));
                try {
                    while ((line = reader.readLine()) != null) {
                        this.insertWord(line.trim().toLowerCase(), WordType.Bad);
                    }
                }
                finally {
                    reader.close();
                }
            }
            catch (FileNotFoundException e) {
                DebugLog.General.error("Can't open file with bad words (" + badWordsFilename + ")");
            }
            catch (IOException e) {
                DebugLog.General.printException(e, "Can't load file with bad words", LogSeverity.Error);
            }
        }
    }

    public void buildTree(List<String> words, WordType type) {
        for (String word : words) {
            this.insertWord(word, type);
        }
    }

    private void insertWord(String word, WordType type) {
        TreeNode current = this.root;
        for (char c : word.toCharArray()) {
            if (current.children.get(Character.valueOf(c)) != null) {
                current = current.children.get(Character.valueOf(c));
                if (current.type != WordType.Bad) continue;
                break;
            }
            current.children.put(Character.valueOf(c), new TreeNode(this, c));
            current = current.children.get(Character.valueOf(c));
        }
        current.type = type;
    }

    public void loadReplacementsFromFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename));){
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length != 2) continue;
                char symbol = parts[0].charAt(0);
                String replacementsString = parts[1];
                String[] replacementArray = replacementsString.split(",");
                ArrayList<Character> replacements = new ArrayList<Character>();
                for (String replacement : replacementArray) {
                    if (replacement.isEmpty()) continue;
                    replacements.add(Character.valueOf(replacement.charAt(0)));
                }
                SYMBOL_REPLACEMENTS.put(Character.valueOf(symbol), replacements);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<SearchResult> searchText(String text) {
        ArrayList<SearchResult> goodResults = new ArrayList<SearchResult>();
        ArrayList<SearchResult> results = new ArrayList<SearchResult>();
        if (text == null) {
            return results;
        }
        for (int i = 0; i < text.length(); ++i) {
            if (SKIP_SYMBOL.contains(Character.valueOf(text.charAt(i)))) continue;
            this.searchRecursive(this.root, text.toLowerCase(), i, i, results, goodResults, 0);
            if (results.isEmpty()) continue;
            SearchResult result = (SearchResult)results.get(results.size() - 1);
            if (result.endPosition <= i) continue;
            i = result.endPosition;
        }
        for (SearchResult r : goodResults) {
            results.removeIf(searchResult -> searchResult.startPosition >= r.startPosition && searchResult.endPosition <= r.endPosition);
        }
        return results;
    }

    public boolean detectBadWords(String text) {
        ArrayList<SearchResult> goodResults = new ArrayList<SearchResult>();
        ArrayList<SearchResult> results = new ArrayList<SearchResult>();
        for (int i = 0; i < text.length(); ++i) {
            if (SKIP_SYMBOL.contains(Character.valueOf(text.charAt(i)))) continue;
            this.searchRecursive(this.root, text.toLowerCase(), i, i, results, goodResults, 0);
            for (SearchResult r : goodResults) {
                results.removeIf(searchResult -> searchResult.startPosition >= r.startPosition && searchResult.endPosition <= r.endPosition);
            }
            if (results.isEmpty()) continue;
            return true;
        }
        return false;
    }

    public String hideBadWords(String text, List<SearchResult> badWordsList, String hideChar) {
        if (text == null || text.isEmpty() || badWordsList == null || badWordsList.isEmpty()) {
            return text;
        }
        StringBuilder result = new StringBuilder(text);
        for (int i = badWordsList.size() - 1; i >= 0; --i) {
            int wordEnd;
            int wordStart;
            SearchResult resultItem = badWordsList.get(i);
            int start = resultItem.startPosition;
            int end = resultItem.endPosition;
            if (start < 0 || end > result.length() || start > end) continue;
            for (wordStart = start; wordStart > 0 && !Character.isWhitespace(result.charAt(wordStart - 1)); --wordStart) {
            }
            for (wordEnd = end; wordEnd < result.length() && !Character.isWhitespace(result.charAt(wordEnd)); ++wordEnd) {
            }
            if (hideChar.length() == 1) {
                for (int k = wordStart; k < wordEnd; ++k) {
                    result.setCharAt(k, hideChar.charAt(0));
                }
                continue;
            }
            result.delete(wordStart, wordEnd);
            result.insert(wordStart, hideChar);
        }
        return result.toString();
    }

    private void searchRecursive(TreeNode node, String text, int startPos, int currentPos, List<SearchResult> results, List<SearchResult> goodResults, int universalCharacters) {
        block7: {
            char currentChar;
            block8: {
                block6: {
                    SearchResult result;
                    if (currentPos >= text.length() || universalCharacters > 3) {
                        return;
                    }
                    currentChar = text.charAt(currentPos);
                    TreeNode nextNode = node.children.get(Character.valueOf(currentChar));
                    if (nextNode == null) break block6;
                    if (nextNode.type == WordType.Bad) {
                        result = new SearchResult(startPos, currentPos);
                        results.add(result);
                    }
                    if (nextNode.type == WordType.Good) {
                        result = new SearchResult(startPos, currentPos);
                        goodResults.add(result);
                    }
                    this.searchRecursive(nextNode, text, startPos, currentPos + 1, results, goodResults, universalCharacters);
                    break block7;
                }
                if (startPos == currentPos || !UNIVERSAL_SYMBOL.contains(Character.valueOf(currentChar))) break block8;
                for (TreeNode node2 : node.children.values()) {
                    if (node2.type == WordType.Bad && currentPos - startPos > universalCharacters && node2.value == currentChar) {
                        SearchResult result = new SearchResult(startPos, currentPos);
                        results.add(result);
                    }
                    this.searchRecursive(node2, text, startPos, currentPos + 1, results, goodResults, universalCharacters + 1);
                }
                break block7;
            }
            if (!SYMBOL_REPLACEMENTS.containsKey(Character.valueOf(currentChar))) break block7;
            for (TreeNode node2 : node.children.values()) {
                if (!SYMBOL_REPLACEMENTS.get(Character.valueOf(currentChar)).contains(Character.valueOf(node2.value))) continue;
                this.searchRecursive(node2, text, startPos, currentPos + 1, results, goodResults, universalCharacters);
            }
        }
    }

    public void saveTreeToFile(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename));){
            this.saveNode(writer, this.root);
        }
    }

    private void saveNode(BufferedWriter writer, TreeNode node) throws IOException {
        writer.write(node.value);
        writer.write(node.type.ordinal());
        writer.write(node.children.size());
        for (TreeNode child : node.children.values()) {
            this.saveNode(writer, child);
        }
    }

    public void loadTreeFromFile(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename));){
            this.root = this.loadNode(reader);
        }
    }

    private TreeNode loadNode(BufferedReader reader) throws IOException {
        int value = reader.read();
        WordType type = WordType.values()[reader.read()];
        TreeNode node = new TreeNode(this, (char)value);
        node.type = type;
        int childCount = reader.read();
        for (int i = 0; i < childCount; ++i) {
            TreeNode child = this.loadNode(reader);
            node.children.put(Character.valueOf(child.value), child);
        }
        return node;
    }

    static {
        SKIP_SYMBOL = new HashSet();
        UNIVERSAL_SYMBOL = new HashSet();
        SYMBOL_REPLACEMENTS = new HashMap<Character, List<Character>>();
        SKIP_SYMBOL.add(Character.valueOf('*'));
        SKIP_SYMBOL.add(Character.valueOf(' '));
        SKIP_SYMBOL.add(Character.valueOf('.'));
        SKIP_SYMBOL.add(Character.valueOf(','));
        SKIP_SYMBOL.add(Character.valueOf('!'));
        SKIP_SYMBOL.add(Character.valueOf('@'));
        SKIP_SYMBOL.add(Character.valueOf('$'));
        SKIP_SYMBOL.add(Character.valueOf('#'));
        SKIP_SYMBOL.add(Character.valueOf('%'));
        SKIP_SYMBOL.add(Character.valueOf('&'));
        SKIP_SYMBOL.add(Character.valueOf('~'));
        UNIVERSAL_SYMBOL.add(Character.valueOf('*'));
        UNIVERSAL_SYMBOL.add(Character.valueOf('@'));
        UNIVERSAL_SYMBOL.add(Character.valueOf('#'));
        UNIVERSAL_SYMBOL.add(Character.valueOf('$'));
        UNIVERSAL_SYMBOL.add(Character.valueOf('%'));
        UNIVERSAL_SYMBOL.add(Character.valueOf('&'));
        UNIVERSAL_SYMBOL.add(Character.valueOf('!'));
    }

    class TreeNode {
        char value;
        WordType type;
        Map<Character, TreeNode> children;

        public TreeNode(WordsFilter this$0, char value) {
            Objects.requireNonNull(this$0);
            this.value = value;
            this.type = WordType.None;
            this.children = new HashMap<Character, TreeNode>();
        }
    }

    public static enum WordType {
        None,
        Good,
        Bad;

    }

    public static class SearchResult {
        public final int startPosition;
        public final int endPosition;

        public SearchResult(int startPosition, int endPosition) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }

        public String toString() {
            return String.format("Match[%d-%d]'", this.startPosition, this.endPosition);
        }
    }

    public static class Policy {
        public static final int Ban = 1;
        public static final int Kick = 2;
        public static final int Log = 3;
        public static final int Mute = 4;
        public static final int Nothing = 5;

        public static String name(int value) {
            return switch (value) {
                case 1 -> "Ban";
                case 2 -> "Kick";
                case 3 -> "Log";
                case 4 -> "Mute";
                case 5 -> "Nothing";
                default -> "Unknown";
            };
        }
    }
}

