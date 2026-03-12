/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.AmbientStreamManager;
import zombie.audio.FMODAmbientWallLevelData;
import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.utils.BooleanGrid;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;

public final class ParameterInside
extends FMODGlobalParameter {
    private static final FloodFill floodFill = new FloodFill();

    public ParameterInside() {
        super("Inside");
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        if (character == null) {
            return 0.0f;
        }
        IsoGridSquare current = character.getCurrentSquare();
        if (current == null) {
            return 0.0f;
        }
        int playerIndex = ((IsoPlayer)character).getIndex();
        if (current.isInARoom()) {
            return this.calculateInsideFraction(playerIndex, current);
        }
        if (current.haveRoof) {
            for (int z = current.getZ() - 1; z >= 0; --z) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(current.getX(), current.getY(), z);
                if (square == null || square.getRoom() == null) continue;
                return this.calculateInsideFraction(playerIndex, current);
            }
        }
        if (character.getVehicle() == null) {
            return 0.0f;
        }
        return -1.0f;
    }

    public static void renderDebug() {
        if (!DebugOptions.instance.parameterInsideRender.getValue()) {
            return;
        }
        if (FMODAmbientWallLevelData.isOutside(IsoCamera.frameState.camCharacterSquare)) {
            return;
        }
        if (IsoCamera.frameState.camCharacterSquare == null) {
            return;
        }
        floodFill.reset();
        floodFill.calculate(IsoCamera.frameState.playerIndex, IsoCamera.frameState.camCharacterSquare);
        float f = 0.05f;
        for (int i = 0; i < ParameterInside.floodFill.choices.size(); ++i) {
            IsoGridSquare square = ParameterInside.floodFill.choices.get(i);
            if (!ParameterInside.floodFill.outsideAdjacent.getValue(floodFill.gridX(square.x), floodFill.gridY(square.y))) {
                LineDrawer.addRect((float)square.x + 0.05f, (float)square.y + 0.05f, square.z, 0.9f, 0.9f, 0.5f, 0.5f, 0.5f);
                continue;
            }
            IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
            if (FMODAmbientWallLevelData.isOutside(squareN) && FMODAmbientWallLevelData.passesSoundNorth(square, true) || FMODAmbientWallLevelData.isOutside(squareS) && FMODAmbientWallLevelData.passesSoundNorth(squareS, true) || FMODAmbientWallLevelData.isOutside(squareW) && FMODAmbientWallLevelData.passesSoundWest(square, true) || FMODAmbientWallLevelData.isOutside(squareE) && FMODAmbientWallLevelData.passesSoundWest(squareE, true)) {
                LineDrawer.addRect((float)square.x + 0.05f, (float)square.y + 0.05f, square.z, 0.9f, 0.9f, 0.0f, 1.0f, 0.0f);
                continue;
            }
            LineDrawer.addRect((float)square.x + 0.05f, (float)square.y + 0.05f, square.z, 0.9f, 0.9f, 1.0f, 1.0f, 1.0f);
        }
    }

    float calculateInsideFraction(int playerIndex, IsoGridSquare playerSquare) {
        if (playerSquare == null || FMODAmbientWallLevelData.isOutside(playerSquare)) {
            return 0.0f;
        }
        floodFill.reset();
        floodFill.calculate(playerIndex, playerSquare);
        int numOutsideAdjacentSquares = 0;
        int numOutsideExposedSquares = 0;
        for (int i = 0; i < ParameterInside.floodFill.choices.size(); ++i) {
            IsoGridSquare square = ParameterInside.floodFill.choices.get(i);
            if (!ParameterInside.floodFill.outsideAdjacent.getValue(floodFill.gridX(square.x), floodFill.gridY(square.y))) continue;
            ++numOutsideAdjacentSquares;
            IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
            if (!(FMODAmbientWallLevelData.isOutside(squareN) && FMODAmbientWallLevelData.passesSoundNorth(square, false) || FMODAmbientWallLevelData.isOutside(squareS) && FMODAmbientWallLevelData.passesSoundNorth(squareS, false) || FMODAmbientWallLevelData.isOutside(squareW) && FMODAmbientWallLevelData.passesSoundWest(square, false)) && (!FMODAmbientWallLevelData.isOutside(squareE) || !FMODAmbientWallLevelData.passesSoundWest(squareE, false))) continue;
            ++numOutsideExposedSquares;
        }
        return numOutsideAdjacentSquares == 0 ? 1.0f : 1.0f - (float)numOutsideExposedSquares / (float)numOutsideAdjacentSquares;
    }

    public static void calculateFloodFill() {
        IsoPlayer player;
        IsoGridSquare isoGridSquare;
        IsoGameCharacter isoGameCharacter;
        if (AmbientStreamManager.instance.isParameterInsideTrue() && (isoGameCharacter = FMODParameterUtils.getFirstListener()) instanceof IsoPlayer && (isoGridSquare = (player = (IsoPlayer)isoGameCharacter).getCurrentSquare()) instanceof IsoGridSquare) {
            IsoGridSquare square = isoGridSquare;
            floodFill.calculate(player.getPlayerNum(), square);
            return;
        }
        floodFill.reset();
    }

    public static boolean isAdjacentToReachableSquare(IsoGridSquare square, boolean north) {
        int y;
        int x = square.getX();
        if (floodFill.isOutsideAdjacent(x, y = square.getY())) {
            return true;
        }
        return floodFill.isOutsideAdjacent(north ? x : x - 1, north ? y - 1 : y);
    }

    private static final class FloodFill {
        private IsoChunkMap chunkMap;
        private IsoGridSquare start;
        private static final int FLOOD_SIZE = 16;
        private final BooleanGrid visited = new BooleanGrid(16, 16);
        private final BooleanGrid outsideAdjacent = new BooleanGrid(16, 16);
        private final ArrayDeque<IsoGridSquare> stack = new ArrayDeque();
        private final ArrayList<IsoGridSquare> choices = new ArrayList(256);

        private FloodFill() {
        }

        public void calculate(int playerIndex, IsoGridSquare sq) {
            IsoCell cell = IsoWorld.instance.currentCell;
            this.chunkMap = cell.getChunkMap(playerIndex);
            this.start = sq;
            this.push(this.start.getX(), this.start.getY());
            while ((sq = this.pop()) != null) {
                int x = sq.getX();
                int y1 = sq.getY();
                while (this.shouldVisit(x, y1, x, y1 - 1, IsoDirections.N)) {
                    --y1;
                }
                boolean spanLeft = false;
                boolean spanRight = false;
                do {
                    this.visited.setValue(this.gridX(x), this.gridY(y1), true);
                    IsoGridSquare sq2 = this.chunkMap.getGridSquare(x, y1, this.start.getZ());
                    if (sq2 != null) {
                        this.choices.add(sq2);
                    }
                    if (!spanLeft && this.shouldVisit(x, y1, x - 1, y1, IsoDirections.W)) {
                        this.push(x - 1, y1);
                        spanLeft = true;
                    } else if (spanLeft && !this.shouldVisit(x, y1, x - 1, y1, IsoDirections.W)) {
                        spanLeft = false;
                    } else if (spanLeft && !this.shouldVisit(x - 1, y1, x - 1, y1 - 1, IsoDirections.N)) {
                        this.push(x - 1, y1);
                    }
                    if (!spanRight && this.shouldVisit(x, y1, x + 1, y1, IsoDirections.E)) {
                        this.push(x + 1, y1);
                        spanRight = true;
                        continue;
                    }
                    if (spanRight && !this.shouldVisit(x, y1, x + 1, y1, IsoDirections.E)) {
                        spanRight = false;
                        continue;
                    }
                    if (!spanRight || this.shouldVisit(x + 1, y1, x + 1, y1 - 1, IsoDirections.N)) continue;
                    this.push(x + 1, y1);
                } while (this.shouldVisit(x, ++y1 - 1, x, y1, IsoDirections.S));
            }
        }

        boolean shouldVisit(int x1, int y1, int x2, int y2, IsoDirections dir) {
            if (this.gridX(x2) >= 16 || this.gridX(x2) < 0) {
                return false;
            }
            if (this.gridY(y2) >= 16 || this.gridY(y2) < 0) {
                return false;
            }
            if (this.visited.getValue(this.gridX(x2), this.gridY(y2))) {
                return false;
            }
            IsoGridSquare square2 = this.chunkMap.getGridSquare(x2, y2, this.start.getZ());
            if (square2 == null) {
                return false;
            }
            if (FMODAmbientWallLevelData.isOutside(square2)) {
                this.outsideAdjacent.setValue(this.gridX(x1), this.gridY(y1), true);
                return false;
            }
            IsoGridSquare square1 = this.chunkMap.getGridSquare(x1, y1, this.start.getZ());
            switch (dir) {
                case N: {
                    return FMODAmbientWallLevelData.passesSoundNorth(square1, false);
                }
                case S: {
                    return FMODAmbientWallLevelData.passesSoundNorth(square2, false);
                }
                case W: {
                    return FMODAmbientWallLevelData.passesSoundWest(square1, false);
                }
                case E: {
                    return FMODAmbientWallLevelData.passesSoundWest(square2, false);
                }
            }
            throw new IllegalArgumentException("unhandled direction");
        }

        void push(int x, int y) {
            IsoGridSquare sq = this.chunkMap.getGridSquare(x, y, this.start.getZ());
            this.stack.push(sq);
        }

        IsoGridSquare pop() {
            return this.stack.isEmpty() ? null : this.stack.pop();
        }

        int gridX(int x) {
            return x - (this.start.getX() - 8);
        }

        int gridY(int y) {
            return y - (this.start.getY() - 8);
        }

        boolean isOutsideAdjacent(int x, int y) {
            return this.outsideAdjacent.getValue(this.gridX(x), this.gridY(y));
        }

        public void reset() {
            this.choices.clear();
            this.stack.clear();
            this.visited.clear();
            this.outsideAdjacent.clear();
        }
    }
}

