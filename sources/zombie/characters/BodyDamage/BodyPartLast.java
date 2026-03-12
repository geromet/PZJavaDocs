/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.BodyDamage;

import zombie.characters.BodyDamage.BodyPart;

public class BodyPartLast {
    private boolean bandaged;
    private boolean bitten;
    private boolean scratched;
    private boolean cut;
    private boolean dirtyBandage;

    public boolean bandaged() {
        return this.bandaged;
    }

    public boolean isBandageDirty() {
        return this.dirtyBandage;
    }

    public boolean bitten() {
        return this.bitten;
    }

    public boolean scratched() {
        return this.scratched;
    }

    public boolean isCut() {
        return this.cut;
    }

    public void copy(BodyPart other) {
        this.bandaged = other.bandaged();
        this.bitten = other.bitten();
        this.scratched = other.scratched();
        this.cut = other.isCut();
        this.dirtyBandage = other.bandaged() && other.isBandageDirty();
    }
}

