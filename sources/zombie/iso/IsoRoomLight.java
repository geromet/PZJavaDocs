/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.characters.IsoPlayer;
import zombie.core.opengl.RenderSettings;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.IsoRoom;

public final class IsoRoomLight {
    public static int nextId = 1;
    private static final int SHINE_DIST = 5;
    public int id;
    public IsoRoom room;
    public int x;
    public int y;
    public int z;
    public int width;
    public int height;
    public float r;
    public float g;
    public float b;
    public boolean active;
    public boolean activeJni;
    public boolean hydroPowered = true;

    public IsoRoomLight(IsoRoom room, int x, int y, int z, int width, int height) {
        this.room = room;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.r = 0.9f;
        this.b = 0.7f;
        this.active = room.def.lightsActive;
    }

    public void addInfluence() {
        this.r = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8f * IsoGridSquare.rmod * 0.7f;
        this.g = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8f * IsoGridSquare.gmod * 0.7f;
        this.b = RenderSettings.getInstance().getAmbientForPlayer(IsoPlayer.getPlayerIndex()) * 0.8f * IsoGridSquare.bmod * 0.7f;
        this.r *= 2.0f;
        this.g *= 2.0f;
        this.b *= 2.0f;
        this.shineIn(this.x - 1, this.y, this.x, this.y + this.height, 5, 0);
        this.shineIn(this.x, this.y - 1, this.x + this.width, this.y, 0, 5);
        this.shineIn(this.x + this.width, this.y, this.x + this.width + 1, this.y + this.height, -5, 0);
        this.shineIn(this.x, this.y + this.height, this.x + this.width, this.y + this.height + 1, 0, -5);
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        this.active = this.room.def.lightsActive;
        if (!(sq != null || !this.hydroPowered || sq.hasGridPower() || sq != null && sq.haveElectricity())) {
            this.active = false;
            return;
        }
        if (!this.active) {
            return;
        }
        this.r = 0.9f;
        this.g = 0.8f;
        this.b = 0.7f;
        for (int yy = this.y; yy < this.y + this.height; ++yy) {
            for (int xx = this.x; xx < this.x + this.width; ++xx) {
                sq = IsoWorld.instance.currentCell.getGridSquare(xx, yy, this.z);
                if (sq == null) continue;
                sq.setLampostTotalR(sq.getLampostTotalR() + this.r);
                sq.setLampostTotalG(sq.getLampostTotalG() + this.g);
                sq.setLampostTotalB(sq.getLampostTotalB() + this.b);
            }
        }
        this.shineOut(this.x, this.y, this.x + 1, this.y + this.height, -5, 0);
        this.shineOut(this.x, this.y, this.x + this.width, this.y + 1, 0, -5);
        this.shineOut(this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, 5, 0);
        this.shineOut(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, 0, 5);
    }

    private void shineOut(int x1, int y1, int x2, int y2, int distX, int distY) {
        for (int y = y1; y < y2; ++y) {
            for (int x = x1; x < x2; ++x) {
                this.shineOut(x, y, distX, distY);
            }
        }
    }

    private void shineOut(int x, int y, int distX, int distY) {
        block5: {
            block7: {
                block6: {
                    block4: {
                        if (distX <= 0) break block4;
                        for (int i = 1; i <= distX; ++i) {
                            this.shineFromTo(x, y, x + i, y);
                        }
                        break block5;
                    }
                    if (distX >= 0) break block6;
                    for (int i = 1; i <= -distX; ++i) {
                        this.shineFromTo(x, y, x - i, y);
                    }
                    break block5;
                }
                if (distY <= 0) break block7;
                for (int i = 1; i <= distY; ++i) {
                    this.shineFromTo(x, y, x, y + i);
                }
                break block5;
            }
            if (distY >= 0) break block5;
            for (int i = 1; i <= -distY; ++i) {
                this.shineFromTo(x, y, x, y - i);
            }
        }
    }

    private void shineFromTo(int x1, int y1, int x2, int y2) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x2, y2, this.z);
        if (sq == null) {
            return;
        }
        if (sq.getRoom() == this.room) {
            return;
        }
        LosUtil.TestResults test = LosUtil.lineClear(IsoWorld.instance.currentCell, x1, y1, this.z, x2, y2, this.z, false);
        if (test == LosUtil.TestResults.Blocked) {
            return;
        }
        float dist = Math.abs(x1 - x2) + Math.abs(y1 - y2);
        float del = dist / 5.0f;
        del = 1.0f - del;
        del *= del;
        float totR = del * this.r * 2.0f;
        float totG = del * this.g * 2.0f;
        float totB = del * this.b * 2.0f;
        sq.setLampostTotalR(sq.getLampostTotalR() + totR);
        sq.setLampostTotalG(sq.getLampostTotalG() + totG);
        sq.setLampostTotalB(sq.getLampostTotalB() + totB);
    }

    private void shineIn(int x1, int y1, int x2, int y2, int distX, int distY) {
        for (int y = y1; y < y2; ++y) {
            for (int x = x1; x < x2; ++x) {
                this.shineIn(x, y, distX, distY);
            }
        }
    }

    private void shineIn(int x, int y, int distX, int distY) {
        block6: {
            block8: {
                block7: {
                    block5: {
                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, this.z);
                        if (sq == null || !sq.has(IsoFlagType.exterior)) {
                            return;
                        }
                        if (distX <= 0) break block5;
                        for (int i = 1; i <= distX; ++i) {
                            this.shineFromToIn(x, y, x + i, y);
                        }
                        break block6;
                    }
                    if (distX >= 0) break block7;
                    for (int i = 1; i <= -distX; ++i) {
                        this.shineFromToIn(x, y, x - i, y);
                    }
                    break block6;
                }
                if (distY <= 0) break block8;
                for (int i = 1; i <= distY; ++i) {
                    this.shineFromToIn(x, y, x, y + i);
                }
                break block6;
            }
            if (distY >= 0) break block6;
            for (int i = 1; i <= -distY; ++i) {
                this.shineFromToIn(x, y, x, y - i);
            }
        }
    }

    private void shineFromToIn(int x1, int y1, int x2, int y2) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x2, y2, this.z);
        if (sq == null) {
            return;
        }
        LosUtil.TestResults test = LosUtil.lineClear(IsoWorld.instance.currentCell, x1, y1, this.z, x2, y2, this.z, false);
        if (test == LosUtil.TestResults.Blocked) {
            return;
        }
        float dist = Math.abs(x1 - x2) + Math.abs(y1 - y2);
        float del = dist / 5.0f;
        del = 1.0f - del;
        del *= del;
        float totR = del * this.r * 2.0f;
        float totG = del * this.g * 2.0f;
        float totB = del * this.b * 2.0f;
        sq.setLampostTotalR(sq.getLampostTotalR() + totR);
        sq.setLampostTotalG(sq.getLampostTotalG() + totG);
        sq.setLampostTotalB(sq.getLampostTotalB() + totB);
    }

    public void clearInfluence() {
        for (int yy = this.y - 5; yy < this.y + this.height + 5; ++yy) {
            for (int xx = this.x - 5; xx < this.x + this.width + 5; ++xx) {
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(xx, yy, this.z);
                if (sq == null) continue;
                sq.setLampostTotalR(0.0f);
                sq.setLampostTotalG(0.0f);
                sq.setLampostTotalB(0.0f);
            }
        }
    }

    public boolean isInBounds() {
        IsoChunkMap[] chunkMap = IsoWorld.instance.currentCell.chunkMap;
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            if (chunkMap[pn].ignore) continue;
            int minX = chunkMap[pn].getWorldXMinTiles();
            int maxX = chunkMap[pn].getWorldXMaxTiles();
            int minY = chunkMap[pn].getWorldYMinTiles();
            int maxY = chunkMap[pn].getWorldYMaxTiles();
            if (this.x - 5 >= maxX || this.x + this.width + 5 <= minX || this.y - 5 >= maxY || this.y + this.height + 5 <= minY) continue;
            return true;
        }
        return false;
    }
}

