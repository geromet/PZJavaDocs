/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.util.ArrayList;
import zombie.characters.animals.AnimalCell;
import zombie.characters.animals.AnimalChunk;
import zombie.characters.animals.AnimalManagerMain;
import zombie.characters.animals.AnimalZones;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.VirtualAnimal;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.iso.SaveBufferMap;

public final class AnimalManagerWorker {
    private static AnimalManagerWorker instance = new AnimalManagerWorker();
    protected static final int SQUARES_PER_CHUNK = 8;
    protected static final int CHUNKS_PER_CELL = 32;
    protected static final int SQUARES_PER_CELL = 256;
    int minX;
    int minY;
    int width;
    int height;
    boolean server;
    boolean client;
    AnimalCell[] cells;
    final ArrayList<AnimalCell> loadedCells = new ArrayList();

    public static AnimalManagerWorker getInstance() {
        if (instance == null) {
            instance = new AnimalManagerWorker();
        }
        return instance;
    }

    void init(boolean bClient, boolean bServer, int minX, int minY, int width, int height) {
        this.server = bServer;
        this.client = bClient;
        this.minX = minX;
        this.minY = minY;
        this.width = width;
        this.height = height;
        this.cells = new AnimalCell[this.width * this.height];
        if (bClient) {
            return;
        }
        for (int i = 0; i < this.cells.length; ++i) {
            this.allocCell(i);
        }
    }

    public void allocCell(int index) {
        this.cells[index] = AnimalCell.alloc().init(this.minX + index % this.width, this.minY + index / this.width);
        AnimalZones.createJunctions(this.cells[index]);
    }

    public void allocCell(int cellX, int cellY) {
        AnimalCell cell = this.getCellFromCellPos(cellX, cellY);
        if (cell != null) {
            cell.addedJunctions = false;
            AnimalZones.createJunctions(cell);
            return;
        }
        this.allocCell(cellX - this.minX + (cellY - this.minY) * this.width);
    }

    void saveRealAnimals(ArrayList<IsoAnimal> animals) {
        for (int i = 0; i < animals.size(); ++i) {
            IsoAnimal animal = animals.get(i);
            this.checkLastCellSavedTo(animal);
            AnimalCell cell = this.getCellFromSquarePos(PZMath.fastfloor(animal.getX()), PZMath.fastfloor(animal.getY()));
            if (cell == null) continue;
            this.loadIfNeeded(cell);
            VirtualAnimal virtualAnimal = new VirtualAnimal();
            virtualAnimal.setX(animal.getX());
            virtualAnimal.setY(animal.getY());
            virtualAnimal.setZ(animal.getZ());
            virtualAnimal.moveForwardOnZone = animal.isMoveForwardOnZone();
            virtualAnimal.forwardDirection.set(animal.getForwardDirectionX(), animal.getForwardDirectionY());
            virtualAnimal.id = animal.virtualId;
            virtualAnimal.migrationGroup = animal.migrationGroup;
            virtualAnimal.animals.add(animal);
            if (cell.saveRealAnimalHack == null) {
                cell.saveRealAnimalHack = new ArrayList();
            }
            cell.saveRealAnimalHack.add(virtualAnimal);
            cell.dataChanged = true;
            animal.setLastCellSavedTo(cell.x, cell.y);
        }
    }

    void checkLastCellSavedTo(IsoAnimal animal) {
        int cellX = animal.getLastCellSavedToX();
        int cellY = animal.getLastCellSavedToY();
        if (cellX == Integer.MIN_VALUE || cellY == Integer.MIN_VALUE) {
            return;
        }
        int curCellX = PZMath.fastfloor(animal.getX() / 256.0f);
        int curCellY = PZMath.fastfloor(animal.getY() / 256.0f);
        if (cellX == curCellX && cellY == curCellY) {
            return;
        }
        AnimalCell cell = this.getCellFromCellPos(cellX, cellY);
        if (cell == null) {
            return;
        }
        this.loadIfNeeded(cell);
        cell.dataChanged = true;
    }

    void save() {
        for (int i = 0; i < this.loadedCells.size(); ++i) {
            AnimalCell cell = this.loadedCells.get(i);
            if (!cell.dataChanged) continue;
            cell.dataChanged = false;
            cell.save();
        }
    }

    void saveToBufferMap(SaveBufferMap bufferMap) {
        for (int i = 0; i < this.loadedCells.size(); ++i) {
            AnimalCell cell = this.loadedCells.get(i);
            if (!cell.dataChanged) continue;
            cell.dataChanged = false;
            cell.saveToBufferMap(bufferMap);
        }
    }

    void stop() {
        int i;
        for (i = 0; i < this.loadedCells.size(); ++i) {
            AnimalCell cell = this.loadedCells.get(i);
            if (!cell.dataChanged) continue;
            cell.dataChanged = false;
            cell.unload();
        }
        this.loadedCells.clear();
        if (this.cells == null) {
            return;
        }
        for (i = 0; i < this.cells.length; ++i) {
            this.cells[i].release();
            this.cells[i] = null;
        }
        this.cells = null;
    }

    AnimalCell getCellFromCellPos(int x, int y) {
        if ((x -= this.minX) < 0 || x >= this.width || (y -= this.minY) < 0 || y >= this.height || this.cells == null) {
            return null;
        }
        return this.cells[x + y * this.width];
    }

    AnimalCell getCellFromSquarePos(int x, int y) {
        if ((x -= this.minX * 256) < 0 || (y -= this.minY * 256) < 0) {
            return null;
        }
        int cellX = x / 256;
        int cellY = y / 256;
        if (cellX >= this.width || cellY >= this.height) {
            return null;
        }
        return this.cells[cellX + cellY * this.width];
    }

    AnimalCell getCellFromChunkPos(int x, int y) {
        return this.getCellFromSquarePos(x * 8, y * 8);
    }

    void loadIfNeeded(AnimalCell cell) {
        cell.loadedTime = System.currentTimeMillis();
        if (cell.isLoaded()) {
            return;
        }
        cell.load();
        assert (!this.loadedCells.contains(cell));
        this.loadedCells.add(cell);
    }

    void loadChunk(int x, int y) {
        AnimalCell cell = this.getCellFromChunkPos(x, y);
        if (cell == null) {
            return;
        }
        cell.setChunkLoaded(x, y, true);
        this.loadIfNeeded(cell);
        AnimalChunk chunk = cell.getChunkFromChunkPos(x, y);
        if (chunk == null) {
            return;
        }
        this.passToMain(chunk.animals);
        chunk.animals.clear();
        cell.dataChanged = true;
    }

    void unloadChunk(int x, int y) {
        AnimalCell cell = this.getCellFromChunkPos(x, y);
        if (cell == null) {
            return;
        }
        cell.setChunkLoaded(x, y, false);
    }

    void addAnimal(VirtualAnimal animal) {
        AnimalCell cell = this.getCellFromSquarePos(PZMath.fastfloor(animal.getX()), PZMath.fastfloor(animal.getY()));
        if (cell == null) {
            return;
        }
        this.loadIfNeeded(cell);
        AnimalChunk chunk = cell.getChunkFromSquarePos(PZMath.fastfloor(animal.getX()), PZMath.fastfloor(animal.getY()));
        if (chunk == null) {
            return;
        }
        VirtualAnimal vAnimal = chunk.findAnimalByID(animal.id);
        if (vAnimal == animal) {
            return;
        }
        if (vAnimal != null) {
            for (int j = 0; j < animal.animals.size(); ++j) {
                IsoAnimal isoAnimal = animal.animals.get(j);
                if (vAnimal.animals.contains(isoAnimal)) {
                    DebugLog.Animal.error("Trying to add an existing animal.");
                    animal.animals.remove(j--);
                    continue;
                }
                IsoAnimal isoAnimal1 = vAnimal.findAnimalById(isoAnimal.getAnimalID());
                if (isoAnimal1 == null) continue;
                DebugLog.Animal.error("Trying to add an animal with an existing IsoAnimal.animalID.");
                animal.animals.remove(j--);
            }
            vAnimal.animals.addAll(animal.animals);
            cell.dataChanged = true;
            return;
        }
        chunk.animals.add(animal);
        cell.dataChanged = true;
        if (chunk.animals.size() + chunk.animalTracks.size() == 1) {
            AnimalZones.addAnimalChunk(chunk);
        }
    }

    void removeFromWorld(IsoAnimal animal) {
        int cellX = animal.getLastCellSavedToX();
        int cellY = animal.getLastCellSavedToY();
        if (cellX == Integer.MIN_VALUE || cellY == Integer.MIN_VALUE) {
            return;
        }
        AnimalCell cell = this.getCellFromCellPos(cellX, cellY);
        if (cell == null) {
            return;
        }
        this.loadIfNeeded(cell);
        cell.dataChanged = true;
    }

    public AnimalChunk getAnimalChunk(float x, float y) {
        AnimalCell cell = this.getCellFromSquarePos(PZMath.fastfloor(x), PZMath.fastfloor(y));
        if (cell == null) {
            return null;
        }
        this.loadIfNeeded(cell);
        return cell.getChunkFromSquarePos(PZMath.fastfloor(x), PZMath.fastfloor(y));
    }

    void moveAnimal(VirtualAnimal animal, float x, float y) {
        this.removeFromChunk(animal);
        animal.setX(x);
        animal.setY(y);
        if (this.isChunkLoadedWorldPos(PZMath.fastfloor(x), PZMath.fastfloor(y)) && !this.onEdgeOfLoadedArea(PZMath.fastfloor(x), PZMath.fastfloor(y))) {
            ArrayList<VirtualAnimal> animals = new ArrayList<VirtualAnimal>();
            animals.add(animal);
            animal.setRemoved(true);
            this.passToMain(animals);
            return;
        }
        this.addAnimal(animal);
    }

    void removeFromChunk(VirtualAnimal animal) {
        AnimalCell cell = this.getCellFromSquarePos(PZMath.fastfloor(animal.getX()), PZMath.fastfloor(animal.getY()));
        if (cell == null) {
            return;
        }
        this.loadIfNeeded(cell);
        AnimalChunk chunk = cell.getChunkFromSquarePos(PZMath.fastfloor(animal.getX()), PZMath.fastfloor(animal.getY()));
        if (chunk == null) {
            return;
        }
        if (chunk.animals.size() + chunk.animalTracks.size() == 1) {
            AnimalZones.removeAnimalChunk(chunk);
        }
        chunk.animals.remove(animal);
    }

    void passToMain(ArrayList<VirtualAnimal> animals) {
        AnimalManagerMain.getInstance().fromWorker(animals);
    }

    boolean isChunkLoadedWorldPos(int x, int y) {
        AnimalCell cell = this.getCellFromSquarePos(x, y);
        if (cell == null) {
            return false;
        }
        return cell.isChunkLoadedWorldPos(x, y);
    }

    boolean onEdgeOfLoadedArea(int x, int y) {
        return false;
    }
}

