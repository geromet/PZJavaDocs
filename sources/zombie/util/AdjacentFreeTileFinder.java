/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;

public class AdjacentFreeTileFinder {
    public static boolean isTileOrAdjacent(IsoGridSquare a, IsoGridSquare b) {
        if (a == b) {
            return true;
        }
        if ((float)Math.abs(a.getX() - b.getX()) > 1.0f) {
            return false;
        }
        if ((float)Math.abs(a.getY() - b.getY()) > 1.0f) {
            return false;
        }
        return AdjacentFreeTileFinder.privTrySquare(a, b, null);
    }

    public static IsoGridSquare Find(IsoGridSquare gridSquare, IsoPlayer playerObj, KahluaTable excludeList) {
        if (excludeList == null) {
            excludeList = LuaManager.platform.newTable();
        }
        KahluaTable choices = LuaManager.platform.newTable();
        int choicescount = 1;
        IsoGridSquare a = gridSquare.getAdjacentSquare(IsoDirections.W);
        IsoGridSquare b = gridSquare.getAdjacentSquare(IsoDirections.E);
        IsoGridSquare c = gridSquare.getAdjacentSquare(IsoDirections.N);
        IsoGridSquare d = gridSquare.getAdjacentSquare(IsoDirections.S);
        if (AdjacentFreeTileFinder.privTrySquare(gridSquare, a, excludeList)) {
            choices.rawset(choices.size() + 1, (Object)a);
            ++choicescount;
        }
        if (AdjacentFreeTileFinder.privTrySquare(gridSquare, b, excludeList)) {
            choices.rawset(choices.size() + 1, (Object)b);
            ++choicescount;
        }
        if (AdjacentFreeTileFinder.privTrySquare(gridSquare, c, excludeList)) {
            choices.rawset(choices.size() + 1, (Object)c);
            ++choicescount;
        }
        if (AdjacentFreeTileFinder.privTrySquare(gridSquare, d, excludeList)) {
            choices.rawset(choices.size() + 1, (Object)d);
            ++choicescount;
        }
        if (choicescount == 1) {
            a = gridSquare.getAdjacentSquare(IsoDirections.NW);
            b = gridSquare.getAdjacentSquare(IsoDirections.NE);
            c = gridSquare.getAdjacentSquare(IsoDirections.SW);
            d = gridSquare.getAdjacentSquare(IsoDirections.SE);
            if (AdjacentFreeTileFinder.privTrySquare(gridSquare, a, excludeList)) {
                choices.rawset(choices.size() + 1, (Object)a);
                ++choicescount;
            }
            if (AdjacentFreeTileFinder.privTrySquare(gridSquare, b, excludeList)) {
                choices.rawset(choices.size() + 1, (Object)b);
                ++choicescount;
            }
            if (AdjacentFreeTileFinder.privTrySquare(gridSquare, c, excludeList)) {
                choices.rawset(choices.size() + 1, (Object)c);
                ++choicescount;
            }
            if (AdjacentFreeTileFinder.privTrySquare(gridSquare, d, excludeList)) {
                choices.rawset(choices.size() + 1, (Object)d);
                ++choicescount;
            }
        }
        if (choicescount > 1) {
            float lowestdist = 100000.0f;
            IsoGridSquare distchoice = null;
            KahluaTableIterator iterator2 = choices.iterator();
            while (iterator2.advance()) {
                IsoGridSquare square = (IsoGridSquare)iterator2.getValue();
                float dist = square.DistToProper(playerObj);
                if (!(dist < lowestdist) || !square.canReachTo(gridSquare)) continue;
                lowestdist = dist;
                distchoice = square;
            }
            return distchoice;
        }
        return null;
    }

    private static boolean privTrySquare(IsoGridSquare src, IsoGridSquare test, KahluaTable excludeList) {
        if (src == null || test == null) {
            return false;
        }
        if (excludeList != null) {
            KahluaTableIterator iterator2 = excludeList.iterator();
            while (iterator2.advance()) {
                IsoGridSquare excludeSquare = (IsoGridSquare)iterator2.getValue();
                if (excludeSquare == null || test == null || excludeSquare.getX() != test.getX() || excludeSquare.getY() != test.getY() || excludeSquare.getZ() != test.getZ()) continue;
                return false;
            }
        }
        if (src.getZ() != test.getZ()) {
            return false;
        }
        if (!AdjacentFreeTileFinder.privTrySquareForWalls(src, test)) {
            return false;
        }
        return AdjacentFreeTileFinder.privCanStand(test);
    }

    private static boolean privTrySquareForWalls(IsoGridSquare src, IsoGridSquare test) {
        if (src == null || test == null) {
            return false;
        }
        if (src.getX() < test.getX() && src.getY() == test.getY()) {
            if (test.has("DoorWallW") && !test.isDoorBlockedTo(src)) {
                return true;
            }
            if (test.has(IsoFlagType.cutW) || test.has(IsoFlagType.collideW)) {
                return false;
            }
        }
        if (src.getX() > test.getX() && src.getY() == test.getY()) {
            if (src.has("DoorWallW") && !src.isDoorBlockedTo(test)) {
                return true;
            }
            if (src.has(IsoFlagType.cutW) || src.has(IsoFlagType.collideW)) {
                return false;
            }
        }
        if (src.getY() < test.getY() && src.getX() == test.getX()) {
            if (test.has("DoorWallN") && !test.isDoorBlockedTo(src)) {
                return true;
            }
            if (test.has(IsoFlagType.cutN) || test.has(IsoFlagType.collideN)) {
                return false;
            }
        }
        if (src.getY() > test.getY() && src.getX() == test.getX()) {
            if (src.has("DoorWallN") && !src.isDoorBlockedTo(test)) {
                return true;
            }
            if (src.has(IsoFlagType.cutN) || src.has(IsoFlagType.collideN)) {
                return false;
            }
        }
        return src.getX() == test.getX() || src.getY() == test.getY() || AdjacentFreeTileFinder.privTrySquareForWalls2(src, test.getX(), src.getY(), src.getZ()) && AdjacentFreeTileFinder.privTrySquareForWalls2(src, src.getX(), test.getY(), src.getZ()) && AdjacentFreeTileFinder.privTrySquareForWalls2(test, test.getX(), src.getY(), src.getZ()) && AdjacentFreeTileFinder.privTrySquareForWalls2(test, src.getX(), test.getY(), src.getZ());
    }

    private static boolean privTrySquareForWalls2(IsoGridSquare src, int x, int y, int z) {
        return AdjacentFreeTileFinder.privTrySquareForWalls(src, IsoWorld.instance.getCell().getGridSquare(x, y, z));
    }

    private static boolean privCanStand(IsoGridSquare test) {
        if (test == null) {
            return false;
        }
        if (test.has(IsoFlagType.solid)) {
            return false;
        }
        if (test.has(IsoFlagType.solidtrans)) {
            boolean hasWindowOrFence;
            boolean bl = hasWindowOrFence = test.isAdjacentToWindow() || test.isAdjacentToHoppable();
            if (!hasWindowOrFence) {
                return false;
            }
        }
        return test.TreatAsSolidFloor();
    }
}

