/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoWorld;

public final class ParameterZoneWaterSide
extends FMODGlobalParameter {
    private int playerX = -1;
    private int playerY = -1;
    private int distance = 40;

    public ParameterZoneWaterSide() {
        super("ZoneWaterSide");
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter player = FMODParameterUtils.getFirstListener();
        if (player == null) {
            return 40.0f;
        }
        int playerX = PZMath.fastfloor(player.getX());
        int playerY = PZMath.fastfloor(player.getY());
        if (playerX != this.playerX || playerY != this.playerY) {
            this.playerX = playerX;
            this.playerY = playerY;
            this.distance = this.calculate(player);
            if (this.distance < 40) {
                this.distance = PZMath.clamp(this.distance - 4, 0, 40);
            }
        }
        return this.distance;
    }

    private int calculate(IsoGameCharacter player) {
        if (IsoWorld.instance == null || IsoWorld.instance.currentCell == null || IsoWorld.instance.currentCell.chunkMap[0] == null) {
            return 40;
        }
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[0];
        float closestDistSq = Float.MAX_VALUE;
        for (int y = 0; y < IsoChunkMap.chunkGridWidth; ++y) {
            for (int x = 0; x < IsoChunkMap.chunkGridWidth; ++x) {
                float dy;
                IsoChunk chunk = chunkMap.getChunk(x, y);
                if (chunk == null || chunk.getNumberOfWaterTiles() != 64) continue;
                float centerX = (float)(chunk.wx * 8) + 4.0f;
                float centerY = (float)(chunk.wy * 8) + 4.0f;
                float dx = player.getX() - centerX;
                if (!(dx * dx + (dy = player.getY() - centerY) * dy < closestDistSq)) continue;
                closestDistSq = dx * dx + dy * dy;
            }
        }
        return (int)PZMath.clamp(PZMath.sqrt(closestDistSq), 0.0f, 40.0f);
    }
}

