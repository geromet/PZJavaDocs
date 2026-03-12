/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting;

import java.util.ArrayList;
import zombie.scripting.ScriptBucket;
import zombie.util.StringUtils;

public final class ScriptParser {
    public static final String DEFAULT_INDENTATION = "    ";
    private static final StringBuilder stringBuilder = new StringBuilder();

    public static int readBlock(String s, int start, Block block) {
        int i;
        for (i = start; i < s.length(); ++i) {
            if (s.charAt(i) == '{') {
                Block child = new Block();
                block.children.add(child);
                block.elements.add(child);
                String header = s.substring(start, i).trim();
                String[] ss = header.split("\\s+");
                child.type = ss[0];
                String string = child.id = ss.length > 1 ? ss[1] : null;
                if (ScriptBucket.getCurrentScriptObject() != null) {
                    child.uid = "UID:" + ScriptBucket.getCurrentScriptObject() + "@" + child.type + "@" + i;
                }
                start = i = ScriptParser.readBlock(s, i + 1, child);
                continue;
            }
            if (s.charAt(i) == '}') {
                return i + 1;
            }
            if (s.charAt(i) != ',') continue;
            Value value = new Value();
            value.string = s.substring(start, i);
            block.values.add(value);
            block.elements.add(value);
            start = i + 1;
        }
        return i;
    }

    public static Block parse(String s) {
        Block block = new Block();
        ScriptParser.readBlock(s, 0, block);
        return block;
    }

    public static String stripComments(String totalFile) {
        int start;
        stringBuilder.setLength(0);
        stringBuilder.append(totalFile);
        int end = stringBuilder.lastIndexOf("*/");
        while (end != -1 && (start = stringBuilder.lastIndexOf("/*", end - 1)) != -1) {
            int innerCommentEnd = stringBuilder.lastIndexOf("*/", end - 1);
            while (innerCommentEnd > start) {
                int innerCommentStart = start;
                if ((start = stringBuilder.lastIndexOf("/*", start - 2)) == -1) break;
                innerCommentEnd = stringBuilder.lastIndexOf("*/", innerCommentStart - 2);
            }
            if (start == -1) break;
            stringBuilder.replace(start, end + 2, "");
            end = stringBuilder.lastIndexOf("*/", start);
        }
        totalFile = stringBuilder.toString();
        stringBuilder.setLength(0);
        stringBuilder.trimToSize();
        return totalFile;
    }

    public static ArrayList<String> parseTokens(String totalFile) {
        ArrayList<String> tokens = new ArrayList<String>();
        while (true) {
            int depth = 0;
            int nextindexOfOpen = 0;
            int nextindexOfClosed = 0;
            if (totalFile.indexOf("}", nextindexOfOpen + 1) == -1) break;
            do {
                nextindexOfOpen = totalFile.indexOf("{", nextindexOfOpen + 1);
                if ((nextindexOfClosed = totalFile.indexOf("}", nextindexOfClosed + 1)) < nextindexOfOpen && nextindexOfClosed != -1 || nextindexOfOpen == -1) {
                    nextindexOfOpen = nextindexOfClosed;
                    --depth;
                    continue;
                }
                nextindexOfClosed = nextindexOfOpen;
                ++depth;
            } while (depth > 0);
            tokens.add(totalFile.substring(0, nextindexOfOpen + 1).trim());
            totalFile = totalFile.substring(nextindexOfOpen + 1);
        }
        if (!totalFile.trim().isEmpty()) {
            tokens.add(totalFile.trim());
        }
        return tokens;
    }

    public static class Block
    implements BlockElement {
        public String type;
        public String id;
        public final ArrayList<BlockElement> elements = new ArrayList();
        public final ArrayList<Value> values = new ArrayList();
        public final ArrayList<Block> children = new ArrayList();
        private String uid;
        public String comment;

        public String getUid() {
            return this.uid;
        }

        @Override
        public Block asBlock() {
            return this;
        }

        @Override
        public Value asValue() {
            return null;
        }

        public boolean isEmpty() {
            return this.elements.isEmpty();
        }

        @Override
        public void prettyPrint(int indent, StringBuilder sb, String eol) {
            this.prettyPrint(indent, sb, eol, ScriptParser.DEFAULT_INDENTATION);
        }

        @Override
        public void prettyPrint(int indent, StringBuilder sb, String eol, String indentation) {
            sb.append(indentation.repeat(indent));
            if (!StringUtils.isNullOrWhitespace(this.comment)) {
                sb.append(this.comment);
                sb.append(eol);
                sb.append(indentation.repeat(indent));
            }
            sb.append(this.type);
            if (this.id != null) {
                sb.append(" ");
                sb.append(this.id);
            }
            sb.append(eol);
            sb.append(indentation.repeat(indent));
            sb.append('{');
            sb.append(eol);
            this.prettyPrintElements(indent + 1, sb, eol, indentation);
            sb.append(indentation.repeat(indent));
            sb.append('}');
            sb.append(eol);
        }

        public void prettyPrintElements(int indent, StringBuilder sb, String eol) {
            this.prettyPrintElements(indent, sb, eol, ScriptParser.DEFAULT_INDENTATION);
        }

        public void prettyPrintElements(int indent, StringBuilder sb, String eol, String indentation) {
            BlockElement prev = null;
            for (BlockElement element : this.elements) {
                if (element.asBlock() != null && prev != null) {
                    sb.append(eol);
                }
                if (element.asValue() != null && prev instanceof Block) {
                    sb.append(eol);
                }
                element.prettyPrint(indent, sb, eol, indentation);
                prev = element;
            }
        }

        public Block addBlock(String type, String id) {
            Block block = new Block();
            block.type = type;
            block.id = id;
            this.elements.add(block);
            this.children.add(block);
            return block;
        }

        public Block getBlock(String type, String id) {
            for (Block block : this.children) {
                if (!block.type.equals(type) || (block.id == null || !block.id.equals(id)) && (block.id != null || id != null)) continue;
                return block;
            }
            return null;
        }

        public Value getValue(String key) {
            for (Value value1 : this.values) {
                int p = value1.string.indexOf(61);
                if (p <= 0 || !value1.getKey().trim().equals(key)) continue;
                return value1;
            }
            return null;
        }

        public void setValue(String key, String value) {
            Value value1 = this.getValue(key);
            if (value1 == null) {
                this.addValue(key, value);
            } else {
                value1.string = key + " = " + value;
            }
        }

        public Value addValue(String key, String value) {
            Value value1 = new Value();
            value1.string = key + " = " + value;
            this.elements.add(value1);
            this.values.add(value1);
            return value1;
        }

        public void moveValueAfter(String keyMove, String keyAfter) {
            Value valueMove = this.getValue(keyMove);
            Value valueAfter = this.getValue(keyAfter);
            if (valueMove == null || valueAfter == null) {
                return;
            }
            this.elements.remove(valueMove);
            this.values.remove(valueMove);
            this.elements.add(this.elements.indexOf(valueAfter) + 1, valueMove);
            this.values.add(this.values.indexOf(valueAfter) + 1, valueMove);
        }
    }

    public static class Value
    implements BlockElement {
        public String string;

        @Override
        public Block asBlock() {
            return null;
        }

        @Override
        public Value asValue() {
            return this;
        }

        @Override
        public void prettyPrint(int indent, StringBuilder sb, String eol) {
            this.prettyPrint(indent, sb, eol, ScriptParser.DEFAULT_INDENTATION);
        }

        @Override
        public void prettyPrint(int indent, StringBuilder sb, String eol, String indentation) {
            sb.append(indentation.repeat(indent));
            sb.append(this.string.trim());
            sb.append(',');
            sb.append(eol);
        }

        public String getKey() {
            int p = this.string.indexOf(61);
            return p == -1 ? this.string : this.string.substring(0, p);
        }

        public String getValue() {
            int p = this.string.indexOf(61);
            return p == -1 ? "" : this.string.substring(p + 1);
        }
    }

    public static interface BlockElement {
        public Block asBlock();

        public Value asValue();

        public void prettyPrint(int var1, StringBuilder var2, String var3);

        public void prettyPrint(int var1, StringBuilder var2, String var3, String var4);
    }
}

