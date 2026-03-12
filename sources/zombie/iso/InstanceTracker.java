/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.MatchException
 */
package zombie.iso;

import com.google.common.base.Utf8;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.util.StringUtils;

public abstract class InstanceTracker {
    public static final String DEFAULT = "";
    public static final String ITEMS = "Item Spawns";
    public static final String CONTAINERS = "Container Rolls";
    public static final String STATS = "Stats";
    private static final HashMap<String, TObjectIntHashMap<String>> InstanceGroups = new HashMap();

    public static int get(String group, String key) {
        return InstanceGroups.containsKey(group) ? InstanceGroups.get(group).get(key) : 0;
    }

    public static int get(String key) {
        return InstanceTracker.get(DEFAULT, key);
    }

    public static void set(String group, String key, int value) {
        if (group == null || key == null || key.isBlank()) {
            return;
        }
        InstanceGroups.putIfAbsent(group, new TObjectIntHashMap());
        InstanceGroups.get(group).put(key, value);
    }

    public static void set(String key, int value) {
        InstanceTracker.set(DEFAULT, key, value);
    }

    public static void adj(String group, String key, int value) {
        InstanceGroups.putIfAbsent(group, new TObjectIntHashMap());
        InstanceGroups.get(group).put(key, PZMath.clamp(InstanceGroups.get(group).get(key) + value, Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    public static void adj(String key, int value) {
        InstanceTracker.adj(DEFAULT, key, value);
    }

    public static void inc(String group, String key) {
        InstanceTracker.adj(group, key, 1);
    }

    public static void inc(String key) {
        InstanceTracker.adj(DEFAULT, key, 1);
    }

    public static void dec(String group, String key) {
        InstanceTracker.adj(group, key, -1);
    }

    public static void dec(String key) {
        InstanceTracker.adj(DEFAULT, key, -1);
    }

    public static List<String> sort(String group, Sort sort) {
        return switch (sort.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> InstanceGroups.get(group).keySet().stream().toList();
            case 1 -> InstanceGroups.get(group).keySet().stream().sorted().toList();
            case 2 -> InstanceGroups.get(group).keySet().stream().sorted(Comparator.comparingInt(InstanceGroups.get(group)::get).reversed()).toList();
        };
    }

    public static String exportGroup(String group, Format format, Sort sort) {
        if (!InstanceGroups.containsKey(group)) {
            return DEFAULT;
        }
        StringBuilder sb = new StringBuilder();
        String newLine = System.lineSeparator();
        switch (format.ordinal()) {
            case 0: {
                sb.append("[%s] ; %d entries%s".formatted(group, InstanceGroups.get(group).size(), newLine));
                InstanceTracker.sort(group, sort).forEach(key -> sb.append("%s = %d%s".formatted(key, InstanceTracker.get(group, key), newLine)));
                break;
            }
            case 1: {
                InstanceTracker.sort(group, sort).forEach(key -> sb.append("%s,%s,%d%s".formatted(group, key, InstanceTracker.get(group, key), newLine)));
            }
        }
        sb.append(newLine);
        return sb.toString();
    }

    public static void exportFile(List<String> groups2, String filename, Format format, Sort sort) {
        if (filename.isBlank() || StringUtils.containsDoubleDot(filename) || new File(filename).isAbsolute()) {
            return;
        }
        String path = ZomboidFileSystem.instance.getCacheDir() + File.separator + filename;
        StringBuilder sb = new StringBuilder();
        if (groups2 == null || groups2.isEmpty()) {
            groups2 = InstanceGroups.keySet().stream().toList();
        }
        if (format == Format.CSV) {
            sb.append("group,key,count").append(System.lineSeparator());
        }
        groups2.forEach(group -> sb.append(InstanceTracker.exportGroup(group, format, sort)));
        File file = new File(path);
        try (FileWriter fr = new FileWriter(file);
             BufferedWriter br = new BufferedWriter(fr);){
            br.write(sb.toString());
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public static void save() {
        if (GameClient.client || Core.getInstance().isNoSave()) {
            return;
        }
        try {
            int[] size = new int[]{8};
            InstanceGroups.forEach((group, logs) -> {
                size[0] = size[0] + (6 + Utf8.encodedLength(group));
                logs.forEachKey(key -> {
                    size[0] = size[0] + (6 + Utf8.encodedLength(key));
                    return true;
                });
            });
            ByteBuffer bb = ByteBuffer.allocate(size[0]);
            try {
                bb.putInt(244);
                bb.putInt(InstanceGroups.size());
                InstanceGroups.forEach((group, logs) -> {
                    GameWindow.WriteString(bb, group);
                    bb.putInt(logs.size());
                    logs.forEachEntry((key, count) -> {
                        GameWindow.WriteString(bb, key);
                        bb.putInt(count);
                        return true;
                    });
                });
            }
            catch (BufferOverflowException ex) {
                DebugLog.General.debugln("InstanceTracker Overflow");
                ExceptionLogger.logException(ex);
                return;
            }
            bb.flip();
            File path = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("iTrack.bin"));
            FileOutputStream output = new FileOutputStream(path);
            output.getChannel().truncate(0L);
            output.write(bb.array(), 0, bb.limit());
            output.flush();
            output.close();
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
        InstanceTracker.exportFile(List.of(ITEMS, CONTAINERS), "ItemTracker.log", Format.TEXT, Sort.KEY);
    }

    public static void load() {
        InstanceGroups.clear();
        if (Core.getInstance().isNoSave()) {
            return;
        }
        String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("iTrack.bin");
        File path = new File(fileName);
        if (!path.exists()) {
            return;
        }
        try (FileInputStream inStream = new FileInputStream(path);){
            ByteBuffer bb = ByteBuffer.allocate((int)path.length());
            bb.clear();
            int len = inStream.read(bb.array());
            bb.limit(len);
            int worldVersion = bb.getInt();
            int instanceGroupsSize = bb.getInt();
            for (int i = 0; i < instanceGroupsSize; ++i) {
                String group = GameWindow.ReadString(bb);
                int size = bb.getInt();
                InstanceGroups.put(group, new TObjectIntHashMap());
                for (int j = 0; j < size; ++j) {
                    String key = GameWindow.ReadString(bb);
                    int count = bb.getInt();
                    InstanceTracker.set(group, key, count);
                }
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException(ex);
        }
    }

    public static enum Sort {
        NONE,
        KEY,
        COUNT;

    }

    public static enum Format {
        TEXT,
        CSV;

    }
}

