/*
 * Decompiled with CFR 0.152.
 */
package zombie.pathfind;

import java.util.ArrayList;
import zombie.ai.astar.Mover;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.iso.IsoDirections;
import zombie.pathfind.Chunk;
import zombie.pathfind.MoverType;
import zombie.pathfind.PathFindRequest;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;
import zombie.pathfind.highLevel.HLChunkLevel;
import zombie.pathfind.highLevel.HLLevelTransition;

public final class PMMover {
    public MoverType type;
    public boolean canCrawl;
    public boolean crawling;
    public boolean ignoreCrawlCost;
    public boolean canThump;
    public boolean canClimbFences;
    public boolean canClimbTallFences;
    public int minLevel;
    public int maxLevel;
    public ArrayList<HLChunkLevel> allowedChunkLevels;
    public ArrayList<HLLevelTransition> allowedLevelTransitions;

    public PMMover set(PathFindRequest request) {
        this.crawling = false;
        this.canClimbFences = false;
        this.canClimbTallFences = false;
        Mover mover = request.mover;
        if (mover instanceof IsoAnimal) {
            IsoAnimal isoAnimal = (IsoAnimal)mover;
            this.type = MoverType.Animal;
            this.canClimbFences = isoAnimal.canClimbFences();
        } else if (request.mover instanceof IsoPlayer) {
            this.type = MoverType.Player;
            this.canClimbTallFences = true;
        } else {
            mover = request.mover;
            if (mover instanceof IsoZombie) {
                IsoZombie isoZombie = (IsoZombie)mover;
                this.type = MoverType.Zombie;
                this.crawling = isoZombie.crawling;
            } else {
                throw new IllegalArgumentException("unsupported Mover " + String.valueOf(request.mover));
            }
        }
        this.canCrawl = request.canCrawl;
        this.ignoreCrawlCost = request.ignoreCrawlCost;
        this.canThump = request.canThump;
        this.minLevel = request.minLevel;
        this.maxLevel = request.maxLevel;
        this.allowedChunkLevels = request.allowedChunkLevels;
        this.allowedLevelTransitions = request.allowedLevelTransitions;
        return this;
    }

    public PMMover set(PMMover other) {
        this.type = other.type;
        this.canCrawl = other.canCrawl;
        this.crawling = other.crawling;
        this.ignoreCrawlCost = other.ignoreCrawlCost;
        this.canThump = other.canThump;
        this.canClimbTallFences = other.canClimbTallFences;
        this.minLevel = other.minLevel;
        this.maxLevel = other.maxLevel;
        this.allowedChunkLevels = other.allowedChunkLevels;
        this.allowedLevelTransitions = other.allowedLevelTransitions;
        return this;
    }

    public boolean isAnimal() {
        return this.type == MoverType.Animal;
    }

    public boolean isPlayer() {
        return this.type == MoverType.Player;
    }

    public boolean isZombie() {
        return this.type == MoverType.Zombie;
    }

    public boolean isAllowedChunkLevel(Square square) {
        if (this.allowedChunkLevels == null || this.allowedChunkLevels.isEmpty()) {
            return true;
        }
        Chunk chunk = PolygonalMap2.instance.getChunkFromSquarePos(square.x, square.y);
        HLChunkLevel levelData = chunk.getLevelData(square.z).getHighLevelData();
        return this.allowedChunkLevels.contains(levelData);
    }

    public boolean isAllowedLevelTransition(IsoDirections dir, Square square, boolean bTopFloorSquare) {
        if (this.allowedLevelTransitions == null || this.allowedLevelTransitions.isEmpty()) {
            return true;
        }
        for (int i = 0; i < this.allowedLevelTransitions.size(); ++i) {
            HLLevelTransition levelTransition = this.allowedLevelTransitions.get(i);
            if (!(bTopFloorSquare ? levelTransition.getTopFloorSquare() == square : levelTransition.getBottomFloorSquare() == square)) continue;
            return true;
        }
        return false;
    }
}

