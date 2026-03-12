/*
 * Decompiled with CFR 0.152.
 */
package zombie.vehicles;

public enum TransmissionNumber {
    R(-1),
    N(0),
    Speed1(1),
    Speed2(2),
    Speed3(3),
    Speed4(4),
    Speed5(5),
    Speed6(6),
    Speed7(7),
    Speed8(8);

    private final int index;

    private TransmissionNumber(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    public static TransmissionNumber fromIndex(int index) {
        switch (index) {
            case -1: {
                return R;
            }
            case 0: {
                return N;
            }
            case 1: {
                return Speed1;
            }
            case 2: {
                return Speed2;
            }
            case 3: {
                return Speed3;
            }
            case 4: {
                return Speed4;
            }
            case 5: {
                return Speed5;
            }
            case 6: {
                return Speed6;
            }
            case 7: {
                return Speed7;
            }
            case 8: {
                return Speed8;
            }
        }
        return N;
    }

    public TransmissionNumber getNext(int gearRatioCount) {
        if (this.index == -1 || this.index == gearRatioCount) {
            return this;
        }
        return TransmissionNumber.fromIndex(this.index + 1);
    }

    public TransmissionNumber getPrev(int gearRatioCount) {
        if (this.index == -1 || this.index == gearRatioCount) {
            return this;
        }
        return TransmissionNumber.fromIndex(this.index - 1);
    }

    public String getString() {
        switch (this.index) {
            case -1: {
                return "R";
            }
            case 0: {
                return "N";
            }
            case 1: {
                return "1";
            }
            case 2: {
                return "2";
            }
            case 3: {
                return "3";
            }
            case 4: {
                return "4";
            }
            case 5: {
                return "5";
            }
            case 6: {
                return "6";
            }
            case 7: {
                return "7";
            }
            case 8: {
                return "8";
            }
        }
        return "";
    }
}

