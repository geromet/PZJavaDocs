/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.random;

import org.uncommons.maths.random.SecureRandomSeedGenerator;
import org.uncommons.maths.random.SeedException;
import org.uncommons.maths.random.SeedGenerator;

public class PZSeedGenerator
implements SeedGenerator {
    private static final SeedGenerator[] GENERATORS = new SeedGenerator[]{new SecureRandomSeedGenerator()};

    @Override
    public byte[] generateSeed(int length) {
        for (SeedGenerator generator : GENERATORS) {
            try {
                return generator.generateSeed(length);
            }
            catch (SeedException seedException) {
            }
        }
        throw new IllegalStateException("All available seed generation strategies failed.");
    }
}

