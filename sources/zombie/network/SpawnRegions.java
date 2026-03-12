/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.LuaClosure;
import zombie.Lua.LuaManager;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.scripting.objects.CharacterProfession;

public class SpawnRegions {
    private Region parseRegionTable(KahluaTable regionTable) {
        Object name = regionTable.rawget("name");
        Object file = regionTable.rawget("file");
        Object serverfile = regionTable.rawget("serverfile");
        if (name instanceof String) {
            String nameString = (String)name;
            if (file instanceof String) {
                String fileString = (String)file;
                Region region = new Region();
                region.name = nameString;
                region.file = fileString;
                return region;
            }
        }
        if (name instanceof String) {
            String nameString = (String)name;
            if (serverfile instanceof String) {
                String fileString = (String)serverfile;
                Region region = new Region();
                region.name = nameString;
                region.serverfile = fileString;
                return region;
            }
        }
        return null;
    }

    private ArrayList<Profession> parseProfessionsTable(KahluaTable professionTable) {
        ArrayList<Profession> result = null;
        KahluaTableIterator itProfessions = professionTable.iterator();
        while (itProfessions.advance()) {
            KahluaTable kahluaTable;
            ArrayList<Point> points;
            Object name = itProfessions.getKey();
            Object pointsObj = itProfessions.getValue();
            if (!(name instanceof String)) continue;
            String s = (String)name;
            if (!(pointsObj instanceof KahluaTable) || (points = this.parsePointsTable(kahluaTable = (KahluaTable)pointsObj)) == null) continue;
            Profession profession = new Profession();
            profession.name = s;
            profession.points = points;
            if (result == null) {
                result = new ArrayList<Profession>();
            }
            result.add(profession);
        }
        return result;
    }

    private ArrayList<Point> parsePointsTable(KahluaTable pointsTable) {
        ArrayList<Point> result = null;
        KahluaTableIterator itPoints = pointsTable.iterator();
        while (itPoints.advance()) {
            KahluaTable kahluaTable;
            Point point;
            Object pointObj = itPoints.getValue();
            if (!(pointObj instanceof KahluaTable) || (point = this.parsePointTable(kahluaTable = (KahluaTable)pointObj)) == null) continue;
            if (result == null) {
                result = new ArrayList<Point>();
            }
            result.add(point);
        }
        return result;
    }

    private Point parsePointTable(KahluaTable pointTable) {
        Object posX;
        Object object;
        if (pointTable.rawget("worldX") != null && (object = pointTable.rawget("worldX")) instanceof Double) {
            Double worldX = (Double)object;
            object = pointTable.rawget("worldY");
            if (object instanceof Double) {
                Double worldY = (Double)object;
                object = pointTable.rawget("posX");
                if (object instanceof Double) {
                    posX = (Double)object;
                    object = pointTable.rawget("posY");
                    if (object instanceof Double) {
                        int n;
                        Double posY = (Double)object;
                        Point point = new Point();
                        int cellX = worldX.intValue();
                        int cellY = worldY.intValue();
                        point.posX = cellX * 300 + ((Double)posX).intValue();
                        point.posY = cellY * 300 + posY.intValue();
                        Object object2 = pointTable.rawget("posZ");
                        if (object2 instanceof Double) {
                            Double posZ = (Double)object2;
                            n = posZ.intValue();
                        } else {
                            n = 0;
                        }
                        point.posZ = n;
                        return point;
                    }
                }
            }
        }
        if ((posX = pointTable.rawget("posX")) instanceof Double) {
            Double posX2 = (Double)posX;
            posX = pointTable.rawget("posY");
            if (posX instanceof Double) {
                int n;
                Double posY = (Double)posX;
                Point point = new Point();
                point.posX = posX2.intValue();
                point.posY = posY.intValue();
                object = pointTable.rawget("posZ");
                if (object instanceof Double) {
                    Double posZ = (Double)object;
                    n = posZ.intValue();
                } else {
                    n = 0;
                }
                point.posZ = n;
                return point;
            }
        }
        return null;
    }

    public ArrayList<Region> loadRegionsFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return null;
        }
        try {
            Object[] o;
            LuaManager.env.rawset("SpawnRegions", null);
            LuaManager.loaded.remove(file.getAbsolutePath().replace("\\", "/"));
            LuaManager.RunLua(file.getAbsolutePath());
            Object functionObj = LuaManager.env.rawget("SpawnRegions");
            if (functionObj instanceof LuaClosure && (o = LuaManager.caller.pcall(LuaManager.thread, functionObj, new Object[0])).length > 1 && o[1] instanceof KahluaTable) {
                ArrayList<Region> result = new ArrayList<Region>();
                KahluaTableIterator itRegions = ((KahluaTable)o[1]).iterator();
                while (itRegions.advance()) {
                    KahluaTable kahluaTable;
                    Region region;
                    Object regionObj = itRegions.getValue();
                    if (!(regionObj instanceof KahluaTable) || (region = this.parseRegionTable(kahluaTable = (KahluaTable)regionObj)) == null) continue;
                    result.add(region);
                }
                return result;
            }
            return null;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private String fmtKey(String s) {
        if (((String)s).contains("\\")) {
            s = ((String)s).replace("\\", "\\\\");
        }
        if (((String)s).contains("\"")) {
            s = ((String)s).replace("\"", "\\\"");
        }
        if (((String)s).contains(" ") || ((String)s).contains("\\")) {
            s = "\"" + (String)s + "\"";
        }
        if (((String)s).startsWith("\"")) {
            return "[" + (String)s + "]";
        }
        return s;
    }

    private String fmtValue(String s) {
        if (s.contains("\\")) {
            s = s.replace("\\", "\\\\");
        }
        if (s.contains("\"")) {
            s = s.replace("\"", "\\\"");
        }
        return "\"" + s + "\"";
    }

    public boolean saveRegionsFile(String fileName, ArrayList<Region> regions) {
        boolean bl;
        File file = new File(fileName);
        DebugLog.log("writing " + fileName);
        FileWriter fw = new FileWriter(file);
        try {
            String lineSep = System.lineSeparator();
            fw.write("function SpawnRegions()" + lineSep);
            fw.write("\treturn {" + lineSep);
            for (Region region : regions) {
                if (region.file != null) {
                    fw.write("\t\t{ name = " + this.fmtValue(region.name) + ", file = " + this.fmtValue(region.file) + " }," + lineSep);
                    continue;
                }
                if (region.serverfile != null) {
                    fw.write("\t\t{ name = " + this.fmtValue(region.name) + ", serverfile = " + this.fmtValue(region.serverfile) + " }," + lineSep);
                    continue;
                }
                if (region.professions == null) continue;
                fw.write("\t\t{ name = " + this.fmtValue(region.name) + "," + lineSep);
                fw.write("\t\t\tpoints = {" + lineSep);
                for (Profession profession : region.professions) {
                    fw.write("\t\t\t\t" + this.fmtKey(profession.name) + " = {" + lineSep);
                    for (Point pt : profession.points) {
                        fw.write("\t\t\t\t\t{ posX = " + pt.posX + ", posY = " + pt.posY + ", posZ = " + pt.posZ + " }," + lineSep);
                    }
                    fw.write("\t\t\t\t}," + lineSep);
                }
                fw.write("\t\t\t}" + lineSep);
                fw.write("\t\t}," + lineSep);
            }
            fw.write("\t}" + lineSep);
            fw.write("end" + System.lineSeparator());
            bl = true;
        }
        catch (Throwable throwable) {
            try {
                try {
                    fw.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
        fw.close();
        return bl;
    }

    public ArrayList<Profession> loadPointsFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return null;
        }
        try {
            Object[] o;
            LuaManager.env.rawset("SpawnPoints", null);
            LuaManager.loaded.remove(file.getAbsolutePath().replace("\\", "/"));
            LuaManager.RunLua(file.getAbsolutePath());
            Object functionObj = LuaManager.env.rawget("SpawnPoints");
            if (functionObj instanceof LuaClosure && (o = LuaManager.caller.pcall(LuaManager.thread, functionObj, new Object[0])).length > 1 && o[1] instanceof KahluaTable) {
                return this.parseProfessionsTable((KahluaTable)o[1]);
            }
            return null;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public boolean savePointsFile(String fileName, ArrayList<Profession> professions) {
        boolean bl;
        File file = new File(fileName);
        DebugLog.log("writing " + fileName);
        FileWriter fw = new FileWriter(file);
        try {
            String lineSep = System.lineSeparator();
            fw.write("function SpawnPoints()" + lineSep);
            fw.write("\treturn {" + lineSep);
            for (Profession profession : professions) {
                fw.write("\t\t" + this.fmtKey(profession.name) + " = {" + lineSep);
                for (Point pt : profession.points) {
                    fw.write("\t\t\t{ posX = " + pt.posX + ", posY = " + pt.posY + ", posZ = " + pt.posZ + " }," + lineSep);
                }
                fw.write("\t\t}," + lineSep);
            }
            fw.write("\t}" + lineSep);
            fw.write("end" + System.lineSeparator());
            bl = true;
        }
        catch (Throwable throwable) {
            try {
                try {
                    fw.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }
        fw.close();
        return bl;
    }

    public KahluaTable loadPointsTable(String fileName) {
        ArrayList<Profession> professions = this.loadPointsFile(fileName);
        if (professions == null) {
            return null;
        }
        KahluaTable professionsTable = LuaManager.platform.newTable();
        for (int profIndex = 0; profIndex < professions.size(); ++profIndex) {
            Profession profession = professions.get(profIndex);
            KahluaTable pointsTable = LuaManager.platform.newTable();
            for (int ptIndex = 0; ptIndex < profession.points.size(); ++ptIndex) {
                Point pt = profession.points.get(ptIndex);
                KahluaTable pointTable = LuaManager.platform.newTable();
                pointTable.rawset("posX", (Object)pt.posX);
                pointTable.rawset("posY", (Object)pt.posY);
                pointTable.rawset("posZ", (Object)pt.posZ);
                pointsTable.rawset(ptIndex + 1, (Object)pointTable);
            }
            professionsTable.rawset(profession.name, (Object)pointsTable);
        }
        return professionsTable;
    }

    public boolean savePointsTable(String fileName, KahluaTable professionsTable) {
        ArrayList<Profession> professions = this.parseProfessionsTable(professionsTable);
        if (professions != null) {
            return this.savePointsFile(fileName, professions);
        }
        return false;
    }

    public ArrayList<Region> getDefaultServerRegions() {
        ArrayList<Region> result = new ArrayList<Region>();
        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>(this){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry, new LinkOption[0]) && Files.exists(entry.resolve("spawnpoints.lua"), new LinkOption[0]);
            }
        };
        String mapsPath = ZomboidFileSystem.instance.getMediaPath("maps");
        Path dir = FileSystems.getDefault().getPath(mapsPath, new String[0]);
        if (!Files.exists(dir, new LinkOption[0])) {
            return result;
        }
        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir, (DirectoryStream.Filter<? super Path>)filter);){
            for (Path path : dstrm) {
                Region region = new Region();
                region.name = path.getFileName().toString();
                region.file = "media/maps/" + region.name + "/spawnpoints.lua";
                result.add(region);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public ArrayList<Profession> getDefaultServerPoints() {
        ArrayList<Profession> result = new ArrayList<Profession>();
        Profession profession = new Profession();
        profession.name = CharacterProfession.UNEMPLOYED.getName();
        profession.points = new ArrayList();
        result.add(profession);
        Point point = new Point();
        int cellX = 40;
        int cellY = 22;
        point.posX = 12067;
        point.posY = 6801;
        point.posZ = 0;
        profession.points.add(point);
        return result;
    }

    public static class Region {
        public String name;
        public String file;
        public String serverfile;
        public ArrayList<Profession> professions;
    }

    public static class Profession {
        public String name;
        public ArrayList<Point> points;
    }

    public static class Point {
        public int posX;
        public int posY;
        public int posZ;
    }
}

