/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

public class ExecuteTimeAnalyse {
    String caption;
    TimeStamp[] list;
    int listIndex;

    public ExecuteTimeAnalyse(String caption, int size) {
        this.caption = caption;
        this.list = new TimeStamp[size];
        for (int i = 0; i < size; ++i) {
            this.list[i] = new TimeStamp();
        }
    }

    public void reset() {
        this.listIndex = 0;
    }

    public void add(String comment) {
        this.list[this.listIndex].time = System.nanoTime();
        this.list[this.listIndex].comment = comment;
        ++this.listIndex;
    }

    public long getNanoTime() {
        if (this.listIndex == 0) {
            return 0L;
        }
        return System.nanoTime() - this.list[0].time;
    }

    public int getMsTime() {
        if (this.listIndex == 0) {
            return 0;
        }
        return (int)((System.nanoTime() - this.list[0].time) / 1000000L);
    }

    public void print() {
        long startTime = this.list[0].time;
        System.out.println("---------- START --- " + this.caption + " -------------");
        for (int i = 1; i < this.listIndex; ++i) {
            System.out.println(i + " " + this.list[i].comment + ": " + (this.list[i].time - startTime) / 1000000L);
            startTime = this.list[i].time;
        }
        System.out.println("END: " + (System.nanoTime() - this.list[0].time) / 1000000L);
        System.out.println("----------  END  --- " + this.caption + " -------------");
    }

    static class TimeStamp {
        long time;
        String comment;

        public TimeStamp(String comment) {
            this.comment = comment;
            this.time = System.nanoTime();
        }

        public TimeStamp() {
        }
    }
}

