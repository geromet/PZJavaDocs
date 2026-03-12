/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.properties;

import zombie.iso.IsoDirections;
import zombie.iso.sprite.IsoSprite;
import zombie.seams.SeamFile;
import zombie.seams.SeamManager;

public final class RoofProperties {
    public SeamFile.Tile seamFileTile;

    public static RoofProperties initSprite(IsoSprite sprite) {
        if (sprite == null || sprite.tilesetName == null) {
            return null;
        }
        SeamFile.Tile seamFileTile = SeamManager.getInstance().getHighestPriorityTile(sprite.tilesetName, sprite.tileSheetIndex % 8, sprite.tileSheetIndex / 8);
        if (seamFileTile == null) {
            return null;
        }
        if (seamFileTile.properties != null && seamFileTile.properties.containsKey("master") && (seamFileTile = SeamManager.getInstance().getHighestPriorityTileFromName(seamFileTile.properties.get("master"))) == null) {
            return null;
        }
        RoofProperties rp = new RoofProperties();
        rp.seamFileTile = seamFileTile;
        return rp;
    }

    public boolean hasPossibleSeamSameLevel(IsoDirections dir) {
        return switch (dir) {
            case IsoDirections.E -> {
                if (this.seamFileTile.joinE != null && !this.seamFileTile.joinE.isEmpty()) {
                    yield true;
                }
                yield false;
            }
            case IsoDirections.S -> {
                if (this.seamFileTile.joinS != null && !this.seamFileTile.joinS.isEmpty()) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    public boolean hasPossibleSeamLevelBelow(IsoDirections dir) {
        return switch (dir) {
            case IsoDirections.E -> {
                if (this.seamFileTile.joinBelowE != null && !this.seamFileTile.joinBelowE.isEmpty()) {
                    yield true;
                }
                yield false;
            }
            case IsoDirections.S -> {
                if (this.seamFileTile.joinBelowS != null && !this.seamFileTile.joinBelowS.isEmpty()) {
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    public boolean isJoinedSameLevelEast(RoofProperties rhs) {
        return this.seamFileTile.joinE != null && this.seamFileTile.joinE.contains(rhs.seamFileTile.tileName);
    }

    public boolean isJoinedSameLevelSouth(RoofProperties rhs) {
        return this.seamFileTile.joinS != null && this.seamFileTile.joinS.contains(rhs.seamFileTile.tileName);
    }

    public boolean isJoinedLevelBelowEast(RoofProperties rhs) {
        return this.seamFileTile.joinBelowE != null && this.seamFileTile.joinBelowE.contains(rhs.seamFileTile.tileName);
    }

    public boolean isJoinedLevelBelowSouth(RoofProperties rhs) {
        return this.seamFileTile.joinBelowS != null && this.seamFileTile.joinBelowS.contains(rhs.seamFileTile.tileName);
    }
}

