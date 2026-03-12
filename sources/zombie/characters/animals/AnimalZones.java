/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.ai.states.animals.AnimalZoneState;
import zombie.characters.animals.AnimalCell;
import zombie.characters.animals.AnimalChunk;
import zombie.characters.animals.AnimalManagerMain;
import zombie.characters.animals.AnimalManagerWorker;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.characters.animals.AnimalTracks;
import zombie.characters.animals.AnimalTracksDefinitions;
import zombie.characters.animals.AnimalZone;
import zombie.characters.animals.AnimalZoneJunction;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.MigrationGroupDefinitions;
import zombie.characters.animals.VirtualAnimal;
import zombie.characters.animals.VirtualAnimalState;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;
import zombie.worldMap.UIWorldMap;

public final class AnimalZones {
    private static AnimalZones instance;
    private final Set<AnimalChunk> chunksWithTracksSet = new THashSet<AnimalChunk>();
    private final ArrayList<VirtualAnimal> tempAnimalList = new ArrayList();
    private final Set<VirtualAnimal> doneVirtualAnimals = new THashSet<VirtualAnimal>();
    private static final Set<AnimalChunk> chunksWithTracks;

    public static AnimalZones getInstance() {
        if (instance == null) {
            instance = new AnimalZones();
        }
        return instance;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void addAnimalChunk(AnimalChunk chunk) {
        Set<AnimalChunk> set = chunksWithTracks;
        synchronized (set) {
            chunksWithTracks.add(chunk);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void removeAnimalChunk(AnimalChunk chunk) {
        Set<AnimalChunk> set = chunksWithTracks;
        synchronized (set) {
            chunksWithTracks.remove(chunk);
        }
    }

    public static void createJunctions(AnimalCell cell) {
        if (cell.addedJunctions) {
            return;
        }
        cell.addedJunctions = true;
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaCell metaCell = metaGrid.getCellData(cell.x, cell.y);
        if (metaCell == null) {
            return;
        }
        for (int i = 0; i < metaCell.getAnimalZonesSize(); ++i) {
            AnimalZone zone = metaCell.getAnimalZone(i);
            for (int i2 = i + 1; i2 < metaCell.getAnimalZonesSize(); ++i2) {
                AnimalZone zone2 = metaCell.getAnimalZone(i2);
                if (!zone2.isPolyline()) continue;
                zone.addJunctionsWithOtherZone(zone2);
            }
        }
        AnimalManagerWorker mgrWorker = AnimalManagerWorker.getInstance();
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                IsoMetaCell metaCell2;
                AnimalCell cell2 = mgrWorker.getCellFromCellPos(cell.x + dx, cell.y + dy);
                if (cell == cell2 || cell2 == null || (metaCell2 = metaGrid.getCellData(cell2.x, cell2.y)) == null) continue;
                for (int i = 0; i < metaCell.getAnimalZonesSize(); ++i) {
                    AnimalZone zone = metaCell.getAnimalZone(i);
                    for (int i2 = 0; i2 < metaCell2.getAnimalZonesSize(); ++i2) {
                        AnimalZone zone2 = metaCell2.getAnimalZone(i2);
                        zone.addJunctionsWithOtherZone(zone2);
                    }
                }
            }
        }
    }

    public void spawnAnimalsInCell(AnimalCell cell) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaCell metaCell = metaGrid.getCellData(cell.x, cell.y);
        if (metaCell == null) {
            return;
        }
        for (int i = 0; i < metaCell.getAnimalZonesSize(); ++i) {
            AnimalZone zone = metaCell.getAnimalZone(i);
            this.spawnAnimalsOnZone(zone);
        }
    }

    private void spawnAnimalsOnZone(AnimalZone zone) {
        if (zone.spawnedAnimals || !zone.spawnAnimal) {
            return;
        }
        zone.spawnedAnimals = true;
        if (!"Follow".equals(zone.action)) {
            return;
        }
        Vector2f pos = new Vector2f();
        float t1 = Rand.Next(0.0f, 1.0f);
        if (!zone.getPointOnPolyline(t1, pos)) {
            return;
        }
        VirtualAnimal virtualAnimal = new VirtualAnimal();
        virtualAnimal.x = pos.x;
        virtualAnimal.y = pos.y;
        virtualAnimal.zone = zone;
        MigrationGroupDefinitions.generatePossibleAnimals(virtualAnimal, zone.animalType);
        MigrationGroupDefinitions.initValueFromDef(virtualAnimal);
        if (SandboxOptions.getInstance().animalTrackChance.getValue() > 1) {
            int randomTracks = Rand.Next(6, 12);
            switch (SandboxOptions.getInstance().animalTrackChance.getValue()) {
                case 2: {
                    randomTracks = Rand.Next(0, 2);
                    break;
                }
                case 3: {
                    randomTracks = Rand.Next(2, 4);
                    break;
                }
                case 4: {
                    randomTracks = Rand.Next(2, 7);
                    break;
                }
                case 5: {
                    randomTracks = Rand.Next(4, 10);
                }
            }
            for (int i = 0; i < randomTracks; ++i) {
                String type = switch (Rand.Next(3)) {
                    case 0 -> "poop";
                    case 1 -> "brokentwigs";
                    default -> "footstep";
                };
                long hourInMilli = 3600000L;
                float t2 = Rand.Next(0.0f, 1.0f);
                zone.getPointOnPolyline(t2, pos);
                AnimalZones.addTrack(zone, t2, virtualAnimal, pos, type, hourInMilli *= (long)Rand.Next(1, 5));
            }
            AnimalZones.addTrack(zone, t1, virtualAnimal, "footstep");
        }
        AnimalManagerMain.getInstance().addAnimal(virtualAnimal);
    }

    private static void addTrack(AnimalZone zone, float t2, VirtualAnimal virtualAnimal, Vector2f pos, String type, long hourInMilli) {
        AnimalTracksDefinitions tracksDef;
        zone.getDirectionOnPolyline(t2, virtualAnimal.forwardDirection);
        if (!virtualAnimal.moveForwardOnZone) {
            virtualAnimal.forwardDirection.mul(-1.0f);
        }
        if ((tracksDef = AnimalTracksDefinitions.getTracksDefinition().get(virtualAnimal.migrationGroup)) == null) {
            return;
        }
        AnimalTracks track = AnimalTracks.addAnimalTrackAtPos(virtualAnimal, PZMath.fastfloor(pos.x), PZMath.fastfloor(pos.y), tracksDef.tracks.get(type), hourInMilli);
        AnimalChunk chunk = AnimalManagerWorker.getInstance().getAnimalChunk(track.x, track.y);
        if (chunk != null) {
            chunk.animalTracks.add(track);
            if (chunk.animals.size() + chunk.animalTracks.size() == 1) {
                AnimalZones.addAnimalChunk(chunk);
            }
        }
    }

    private static void addTrack(AnimalZone zone, float t1, VirtualAnimal virtualAnimal, String type) {
        AnimalTracksDefinitions tracksDef;
        zone.getDirectionOnPolyline(t1, virtualAnimal.forwardDirection);
        if (!virtualAnimal.moveForwardOnZone) {
            virtualAnimal.forwardDirection.mul(-1.0f);
        }
        if ((tracksDef = AnimalTracksDefinitions.getTracksDefinition().get(virtualAnimal.migrationGroup)) == null) {
            return;
        }
        AnimalTracks track = AnimalTracks.addAnimalTrack(virtualAnimal, tracksDef.tracks.get(type));
        AnimalChunk chunk = AnimalManagerWorker.getInstance().getAnimalChunk(track.x, track.y);
        if (chunk != null) {
            chunk.animalTracks.add(track);
            if (chunk.animals.size() + chunk.animalTracks.size() == 1) {
                AnimalZones.addAnimalChunk(chunk);
            }
        }
    }

    public static float getClosestZoneDist(float x, float y) {
        AnimalManagerWorker mgrWorker = AnimalManagerWorker.getInstance();
        AnimalCell cell = mgrWorker.getCellFromSquarePos((int)x, (int)y);
        if (cell == null) {
            return -1.0f;
        }
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaCell metaCell = metaGrid.getCellData(cell.x, cell.y);
        if (metaCell == null) {
            return -1.0f;
        }
        Vector2f pos = new Vector2f();
        float closestDist = Float.MAX_VALUE;
        for (int i = 0; i < metaCell.getAnimalZonesSize(); ++i) {
            float dist;
            AnimalZone zone = metaCell.getAnimalZone(i);
            float t = zone.getClosestPointOnPolyline(x, y, pos);
            if (t < 0.0f || !((dist = Vector2f.distance(x, y, pos.x, pos.y)) < closestDist)) continue;
            closestDist = dist;
        }
        return closestDist;
    }

    public AnimalZone getClosestZone(float x, float y, String action) {
        AnimalManagerWorker mgrWorker = AnimalManagerWorker.getInstance();
        AnimalCell cell = mgrWorker.getCellFromSquarePos((int)x, (int)y);
        if (cell == null) {
            return null;
        }
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaCell metaCell = metaGrid.getCellData(cell.x, cell.y);
        if (metaCell == null) {
            return null;
        }
        Vector2f pos = new Vector2f();
        AnimalZone closestZone = null;
        float closestDist = Float.MAX_VALUE;
        for (int i = 0; i < metaCell.getAnimalZonesSize(); ++i) {
            float dist;
            float t;
            AnimalZone zone = metaCell.getAnimalZone(i);
            if (action != null && !action.equals(zone.action) || (t = zone.getClosestPointOnPolyline(x, y, pos)) < 0.0f || !((dist = Vector2f.distance(x, y, pos.x, pos.y)) < closestDist)) continue;
            closestDist = dist;
            closestZone = zone;
        }
        return closestZone;
    }

    public void render(UIWorldMap ui, boolean bAnimals, boolean bTracks) {
        this.renderAnimalCells(ui, bAnimals, bTracks);
        this.renderIsoAnimals(ui);
        this.renderChunkMapBounds(ui);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void updateVirtualAnimals() {
        AnimalZones instance = AnimalZones.getInstance();
        Set<AnimalChunk> set = chunksWithTracks;
        synchronized (set) {
            instance.chunksWithTracksSet.clear();
            instance.chunksWithTracksSet.addAll(chunksWithTracks);
        }
        instance.doneVirtualAnimals.clear();
        for (AnimalChunk chunk : instance.chunksWithTracksSet) {
            instance.tempAnimalList.clear();
            PZArrayUtil.addAll(instance.tempAnimalList, chunk.animals);
            chunk.updateTracks();
            for (int i = 0; i < instance.tempAnimalList.size(); ++i) {
                VirtualAnimal animal = instance.tempAnimalList.get(i);
                if (animal.isRemoved()) {
                    chunk.animals.remove(animal);
                    continue;
                }
                if (!instance.doneVirtualAnimals.add(animal)) continue;
                animal.update();
            }
        }
    }

    private void renderAnimalCells(UIWorldMap ui, boolean bAnimals, boolean bTracks) {
        AnimalPopulationManager mgr = AnimalPopulationManager.getInstance();
        AnimalManagerWorker mgrWorker = AnimalManagerWorker.getInstance();
        for (int cy = mgr.minY; cy <= mgr.minY + mgr.height; ++cy) {
            for (int cx = mgr.minX; cx <= mgr.minX + mgr.width; ++cx) {
                AnimalCell cell = mgrWorker.getCellFromCellPos(cx, cy);
                if (cell == null || !cell.loaded) continue;
                AnimalZones.createJunctions(cell);
                this.renderJunctions(ui, cell);
                for (int chy = 0; chy < 32; ++chy) {
                    for (int chx = 0; chx < 32; ++chx) {
                        String txt;
                        double a;
                        double b;
                        double g;
                        double r;
                        float uiY;
                        float uiX;
                        int i;
                        AnimalChunk chunk = cell.chunks[chx + chy * 32];
                        if (bAnimals) {
                            for (i = 0; i < chunk.animals.size(); ++i) {
                                VirtualAnimal animal = chunk.animals.get(i);
                                uiX = ui.getAPI().worldToUIX(animal.getX(), animal.getY());
                                uiY = ui.getAPI().worldToUIY(animal.getX(), animal.getY());
                                uiX = PZMath.floor(uiX);
                                uiY = PZMath.floor(uiY);
                                r = 0.0;
                                g = 0.0;
                                b = 1.0;
                                a = 1.0;
                                ui.DrawTextureScaledColor(null, (double)uiX - 3.0, (double)uiY - 3.0, 6.0, 6.0, 0.0, 0.0, 1.0, 1.0);
                                if (animal.state == null) continue;
                                txt = Translator.getText("IGUI_MigrationGroup_" + animal.migrationGroup) + " x" + animal.animals.size() + " " + animal.state.getClass().getSimpleName() + (animal.moveForwardOnZone ? " F" : " B");
                                if (animal.state instanceof VirtualAnimalState.StateSleep) {
                                    txt = (String)txt + " " + Math.max(0, (int)Math.floor((animal.wakeTime - GameTime.getInstance().getWorldAgeHours()) * 60.0) + 1) + " mins";
                                }
                                if (animal.state instanceof VirtualAnimalState.StateEat) {
                                    txt = (String)txt + " " + ((int)Math.floor((animal.eatStartTime + (double)((float)animal.timeToEat / 60.0f) - GameTime.getInstance().getWorldAgeHours()) * 60.0) + 1) + " mins";
                                }
                                if (animal.state instanceof VirtualAnimalState.StateFollow) {
                                    txt = (String)txt + "\nNext rest: " + animal.getNextSleepPeriod();
                                    txt = (String)txt + "\nEat: " + animal.getNextEatPeriod();
                                }
                                ui.DrawTextCentre(txt, uiX, (double)uiY + 4.0, 0.0, 0.0, 0.0, 1.0);
                            }
                        }
                        if (!bTracks) continue;
                        for (i = 0; i < chunk.animalTracks.size(); ++i) {
                            AnimalTracks track = chunk.animalTracks.get(i);
                            uiX = ui.getAPI().worldToUIX(track.x, track.y);
                            uiY = ui.getAPI().worldToUIY(track.x, track.y);
                            uiX = PZMath.floor(uiX);
                            uiY = PZMath.floor(uiY);
                            r = 0.33;
                            g = 0.33;
                            b = 0.33;
                            a = 1.0;
                            ui.DrawTextureScaledColor(null, (double)uiX - 3.0, (double)uiY - 3.0, 6.0, 6.0, 0.33, 0.33, 0.33, 1.0);
                            txt = Translator.getText("IGUI_MigrationGroup_" + track.animalType) + " " + AnimalTracks.getTrackStr(track.trackType);
                            if (track.dir != null) {
                                txt = txt + " " + String.valueOf((Object)track.dir);
                            }
                            txt = txt + "\n" + PZMath.fastfloor((GameTime.getInstance().getCalender().getTimeInMillis() - track.addedTime) / 60000L) + " mins ago";
                            ui.DrawTextCentre(txt, uiX, (double)uiY + 4.0, 0.33, 0.33, 0.33, 1.0);
                        }
                    }
                }
            }
        }
    }

    public AnimalZoneJunction getClosestJunction(int x, int y) {
        AnimalZone zone = this.getClosestZone(x, y, null);
        if (zone == null || zone.junctions == null || zone.junctions.isEmpty()) {
            return null;
        }
        AnimalZoneJunction result = zone.junctions.get(0);
        double previousDist = 100.0;
        for (int i = 0; i < zone.junctions.size(); ++i) {
            AnimalZoneJunction test = zone.junctions.get(i);
            double dist = Math.sqrt((test.getY() - y) * (test.getY() - y) + (test.getX() - x) * (test.getX() - x));
            if (!(dist < previousDist) || !(dist < 30.0)) continue;
            previousDist = dist;
            result = test;
        }
        return result;
    }

    private void renderJunctions(UIWorldMap ui, AnimalCell cell) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaCell metaCell = metaGrid.getCellData(cell.x, cell.y);
        if (metaCell == null) {
            return;
        }
        for (int i = 0; i < metaCell.getAnimalZonesSize(); ++i) {
            AnimalZone zone = metaCell.getAnimalZone(i);
            if (zone.junctions == null) continue;
            for (int j = 0; j < zone.junctions.size(); ++j) {
                AnimalZoneJunction junction = zone.junctions.get(j);
                float uiX = ui.getAPI().worldToUIX((float)junction.getX() + 0.5f, (float)junction.getY() + 0.5f);
                float uiY = ui.getAPI().worldToUIY((float)junction.getX() + 0.5f, (float)junction.getY() + 0.5f);
                uiX = PZMath.floor(uiX);
                uiY = PZMath.floor(uiY);
                double r = 0.0;
                double g = 1.0;
                double b = 0.0;
                double a = 1.0;
                ui.DrawTextureScaledColor(null, (double)uiX - 3.0, (double)uiY - 3.0, 6.0, 6.0, 0.0, 1.0, 0.0, 1.0);
                if ("Follow".equals(junction.zoneOther.action) || StringUtils.isNullOrEmpty(junction.zoneOther.action)) continue;
                ui.DrawTextCentre(junction.zoneOther.action, uiX, (double)uiY + 6.0, 0.0, 0.0, 0.0, 1.0);
            }
        }
    }

    private void renderIsoAnimals(UIWorldMap ui) {
        ArrayList<IsoMovingObject> movingObjects = IsoWorld.instance.currentCell.getObjectList();
        for (int i = 0; i < movingObjects.size(); ++i) {
            IsoAnimal animal = Type.tryCastTo(movingObjects.get(i), IsoAnimal.class);
            if (animal == null || animal.isOnHook()) continue;
            float uiX = ui.getAPI().worldToUIX(animal.getX(), animal.getY());
            float uiY = ui.getAPI().worldToUIY(animal.getX(), animal.getY());
            uiX = PZMath.floor(uiX);
            uiY = PZMath.floor(uiY);
            double r = 0.0;
            double g = 0.5;
            double b = 1.0;
            double a = 1.0;
            if (animal.getCurrentSquare() == null) {
                r = 1.0;
                b = 0.0;
                g = 0.0;
            }
            ui.DrawTextureScaledColor(null, (double)uiX - 3.0, (double)uiY - 3.0, 6.0, 6.0, r, g, b, 1.0);
            AnimalZoneState instance = AnimalZoneState.instance();
            if (!animal.isCurrentState(instance)) continue;
            AnimalZoneState.ZoneState subState = animal.get(AnimalZoneState.STATE);
            ui.DrawTextCentre(subState.getClass().getSimpleName() + (animal.isMoveForwardOnZone() ? " F" : " B"), uiX, (double)uiY + 4.0, 0.0, 0.0, 0.0, 1.0);
        }
    }

    private void renderChunkMapBounds(UIWorldMap ui) {
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[0];
        float x1 = ui.getAPI().worldToUIX(chunkMap.getWorldXMinTiles(), chunkMap.getWorldYMinTiles());
        float y1 = ui.getAPI().worldToUIY(chunkMap.getWorldXMinTiles(), chunkMap.getWorldYMinTiles());
        float x2 = ui.getAPI().worldToUIX(chunkMap.getWorldXMaxTiles(), chunkMap.getWorldYMinTiles());
        float y2 = ui.getAPI().worldToUIY(chunkMap.getWorldXMaxTiles(), chunkMap.getWorldYMinTiles());
        float x3 = ui.getAPI().worldToUIX(chunkMap.getWorldXMaxTiles(), chunkMap.getWorldYMaxTiles());
        float y3 = ui.getAPI().worldToUIY(chunkMap.getWorldXMaxTiles(), chunkMap.getWorldYMaxTiles());
        float x4 = ui.getAPI().worldToUIX(chunkMap.getWorldXMinTiles(), chunkMap.getWorldYMaxTiles());
        float y4 = ui.getAPI().worldToUIY(chunkMap.getWorldXMinTiles(), chunkMap.getWorldYMaxTiles());
        x1 = PZMath.floor(x1);
        y1 = PZMath.floor(y1);
        x2 = PZMath.floor(x2);
        y2 = PZMath.floor(y2);
        x3 = PZMath.floor(x3);
        y3 = PZMath.floor(y3);
        x4 = PZMath.floor(x4);
        y4 = PZMath.floor(y4);
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;
        boolean thickness = true;
        ui.DrawLine(Texture.getWhite(), x1, y1, x2, y2, 1.0f, 1.0, 1.0, 1.0, 1.0);
        ui.DrawLine(Texture.getWhite(), x2, y2, x3, y3, 1.0f, 1.0, 1.0, 1.0, 1.0);
        ui.DrawLine(Texture.getWhite(), x3, y3, x4, y4, 1.0f, 1.0, 1.0, 1.0, 1.0);
        ui.DrawLine(Texture.getWhite(), x4, y4, x1, y1, 1.0f, 1.0, 1.0, 1.0, 1.0);
    }

    public static void Reset() {
        chunksWithTracks.clear();
    }

    static {
        chunksWithTracks = new HashSet<AnimalChunk>();
    }
}

