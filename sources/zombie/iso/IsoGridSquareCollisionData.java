/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import zombie.iso.IsoGridSquare;
import zombie.iso.LosUtil;
import zombie.iso.Vector3;

public class IsoGridSquareCollisionData {
    public Vector3 hitPosition = new Vector3();
    public IsoGridSquare isoGridSquare;
    public LosUtil.TestResults testResults = LosUtil.TestResults.Clear;
}

