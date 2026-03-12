/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals.datas;

import zombie.characters.animals.AnimalAllele;
import zombie.characters.animals.IsoAnimal;

public class AnimalGrowStage {
    public int ageToGrow;
    public String nextStage;
    public String nextStageMale;
    public String stage;

    public int getAgeToGrow(IsoAnimal animal) {
        AnimalAllele allele = animal.getUsedGene("ageToGrow");
        float aValue = 1.0f;
        if (allele != null) {
            aValue = allele.currentValue;
        }
        int baseValue = this.ageToGrow;
        float modifier = 0.25f - aValue / 4.0f + 1.0f;
        return (int)((float)baseValue * modifier);
    }
}

