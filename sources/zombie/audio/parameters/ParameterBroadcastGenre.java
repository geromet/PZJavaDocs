/*
 * Decompiled with CFR 0.152.
 */
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;

public class ParameterBroadcastGenre
extends FMODLocalParameter {
    private BroadcastGenre genre = BroadcastGenre.Generic;

    public ParameterBroadcastGenre() {
        super("BroadcastGenre");
    }

    @Override
    public float calculateCurrentValue() {
        return this.genre.label;
    }

    public void setValue(BroadcastGenre genre) {
        this.genre = genre;
    }

    public static enum BroadcastGenre {
        Generic(0),
        News(1),
        EntertainmentNews(2),
        Drama(3),
        KidsShow(4),
        Sports(5),
        MilitaryRadio(6),
        AmateurRadio(7),
        Commercial(8),
        MusicDJ(9),
        GenericVoices(10),
        FranticMilitary(11);

        final int label;

        private BroadcastGenre(int label) {
            this.label = label;
        }

        public int getValue() {
            return this.label;
        }
    }
}

