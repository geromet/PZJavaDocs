/*
 * Decompiled with CFR 0.152.
 */
package zombie.inventory;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;

public final class CompressIdenticalItems {
    private static final int BLOCK_SIZE = 1024;
    private static final ThreadLocal<PerThreadData> perThreadVars = new ThreadLocal<PerThreadData>(){

        @Override
        protected PerThreadData initialValue() {
            return new PerThreadData();
        }
    };

    private static int bufferSize(int size) {
        return (size + 1024 - 1) / 1024 * 1024;
    }

    private static ByteBuffer ensureCapacity(ByteBuffer bb, int capacity) {
        if (bb == null || bb.capacity() < capacity) {
            bb = ByteBuffer.allocate(CompressIdenticalItems.bufferSize(capacity));
        }
        return bb;
    }

    private static ByteBuffer ensureCapacity(ByteBuffer bb) {
        if (bb == null) {
            return ByteBuffer.allocate(1024);
        }
        if (bb.capacity() - bb.position() < 1024) {
            ByteBuffer newBB = CompressIdenticalItems.ensureCapacity(null, bb.position() + 1024);
            return newBB.put(bb.array(), 0, bb.position());
        }
        ByteBuffer newBB = CompressIdenticalItems.ensureCapacity(null, bb.capacity() + 1024);
        return newBB.put(bb.array(), 0, bb.position());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static boolean setCompareItem(PerThreadData perThreadData, InventoryItem item1) throws IOException {
        ByteBuffer bb = perThreadData.itemCompareBuffer;
        bb.clear();
        int itemID1 = item1.id;
        item1.id = 0;
        try {
            while (true) {
                try {
                    bb.putInt(0);
                    item1.save(bb, false);
                    int item1End = bb.position();
                    bb.position(0);
                    bb.putInt(item1End);
                    bb.position(item1End);
                }
                catch (BufferOverflowException ex) {
                    bb = CompressIdenticalItems.ensureCapacity(bb);
                    bb.clear();
                    perThreadData.itemCompareBuffer = bb;
                    continue;
                }
                break;
            }
        }
        finally {
            item1.id = itemID1;
        }
        return true;
    }

    /*
     * Exception decompiling
     */
    private static boolean areItemsIdentical(PerThreadData perThreadData, InventoryItem item1, InventoryItem item2) throws IOException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [1[TRYBLOCK]], but top level block is 7[FORLOOP]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static ArrayList<InventoryItem> save(ByteBuffer output, ArrayList<InventoryItem> items, IsoGameCharacter noCompress) throws IOException {
        PerThreadData perThreadVars = CompressIdenticalItems.perThreadVars.get();
        PerCallData saveVars = perThreadVars.allocSaveVars();
        HashMap<String, ArrayList<InventoryItem>> typeToItems = saveVars.typeToItems;
        ArrayList<String> types = saveVars.types;
        try {
            for (int i = 0; i < items.size(); ++i) {
                String type = items.get(i).getFullType();
                if (!typeToItems.containsKey(type)) {
                    typeToItems.put(type, saveVars.allocItemList());
                    types.add(type);
                }
                typeToItems.get(type).add(items.get(i));
            }
            int posSize = output.position();
            output.putShort((short)0);
            int itemCount = 0;
            for (int k = 0; k < types.size(); ++k) {
                ArrayList<InventoryItem> saveItems = typeToItems.get(types.get(k));
                for (int m = 0; m < saveItems.size(); ++m) {
                    InventoryItem item = saveItems.get(m);
                    saveVars.savedItems.add(item);
                    int identical = 1;
                    int startM = m + 1;
                    if (noCompress == null || !noCompress.isEquipped(item)) {
                        CompressIdenticalItems.setCompareItem(perThreadVars, item);
                        while (m + 1 < saveItems.size() && CompressIdenticalItems.areItemsIdentical(perThreadVars, item, saveItems.get(m + 1))) {
                            saveVars.savedItems.add(saveItems.get(m + 1));
                            ++m;
                            ++identical;
                        }
                    }
                    output.putInt(identical);
                    item.saveWithSize(output, false);
                    if (identical > 1) {
                        for (int i = startM; i <= m; ++i) {
                            output.putInt(saveItems.get((int)i).id);
                        }
                    }
                    ++itemCount;
                }
            }
            int posCurrent = output.position();
            output.position(posSize);
            output.putShort((short)itemCount);
            output.position(posCurrent);
        }
        finally {
            saveVars.next = perThreadVars.saveVars;
            perThreadVars.saveVars = saveVars;
        }
        return saveVars.savedItems;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static ArrayList<InventoryItem> load(ByteBuffer input, int worldVersion, ArrayList<InventoryItem> items, ArrayList<InventoryItem> includingObsoleteItems) throws IOException {
        PerThreadData perThreadVars = CompressIdenticalItems.perThreadVars.get();
        PerCallData saveVars = perThreadVars.allocSaveVars();
        if (items != null) {
            items.clear();
        }
        if (includingObsoleteItems != null) {
            includingObsoleteItems.clear();
        }
        try {
            int count = input.getShort();
            for (int n = 0; n < count; ++n) {
                int i;
                int identical = input.getInt();
                int itemStart = input.position();
                InventoryItem item = InventoryItem.loadItem(input, worldVersion);
                if (item == null) {
                    int idListBytes = identical > 1 ? (identical - 1) * 4 : 0;
                    input.position(input.position() + idListBytes);
                    for (int i2 = 0; i2 < identical; ++i2) {
                        if (includingObsoleteItems != null) {
                            includingObsoleteItems.add(null);
                        }
                        saveVars.savedItems.add(null);
                    }
                    continue;
                }
                for (i = 0; i < identical; ++i) {
                    if (i > 0) {
                        input.position(itemStart);
                        item = InventoryItem.loadItem(input, worldVersion);
                    }
                    if (items != null) {
                        items.add(item);
                    }
                    if (includingObsoleteItems != null) {
                        includingObsoleteItems.add(item);
                    }
                    saveVars.savedItems.add(item);
                }
                for (i = 1; i < identical; ++i) {
                    int id = input.getInt();
                    item = saveVars.savedItems.get(saveVars.savedItems.size() - identical + i);
                    if (item == null) continue;
                    item.id = id;
                }
            }
        }
        finally {
            saveVars.next = perThreadVars.saveVars;
            perThreadVars.saveVars = saveVars;
        }
        return saveVars.savedItems;
    }

    public static void save(ByteBuffer output, InventoryItem item) throws IOException {
        output.putShort((short)1);
        output.putInt(1);
        item.saveWithSize(output, false);
    }

    private static class PerThreadData {
        PerCallData saveVars;
        ByteBuffer itemCompareBuffer = ByteBuffer.allocate(1024);

        private PerThreadData() {
        }

        PerCallData allocSaveVars() {
            if (this.saveVars == null) {
                return new PerCallData();
            }
            PerCallData ret = this.saveVars;
            ret.reset();
            this.saveVars = this.saveVars.next;
            return ret;
        }
    }

    private static class PerCallData {
        final ArrayList<String> types = new ArrayList();
        final HashMap<String, ArrayList<InventoryItem>> typeToItems = new HashMap();
        final ArrayDeque<ArrayList<InventoryItem>> itemLists = new ArrayDeque();
        final ArrayList<InventoryItem> savedItems = new ArrayList();
        PerCallData next;

        private PerCallData() {
        }

        void reset() {
            for (int i = 0; i < this.types.size(); ++i) {
                ArrayList<InventoryItem> itemList = this.typeToItems.get(this.types.get(i));
                itemList.clear();
                this.itemLists.push(itemList);
            }
            this.types.clear();
            this.typeToItems.clear();
            this.savedItems.clear();
        }

        ArrayList<InventoryItem> allocItemList() {
            if (this.itemLists.isEmpty()) {
                return new ArrayList<InventoryItem>();
            }
            return this.itemLists.pop();
        }
    }
}

