/*
 * Decompiled with CFR 0.152.
 */
package zombie.asset;

import java.util.zip.CRC32;

public final class AssetType {
    public static final AssetType INVALID_ASSET_TYPE = new AssetType("");
    public long type;

    public AssetType(String typeName) {
        CRC32 crc32 = new CRC32();
        crc32.update(typeName.getBytes());
        this.type = crc32.getValue();
    }
}

