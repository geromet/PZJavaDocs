/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.io;

public interface BitHeaderRead {
    public int getStartPosition();

    public void read();

    public boolean hasFlags(int var1);

    public boolean hasFlags(long var1);

    public boolean equals(int var1);

    public boolean equals(long var1);

    public int getLen();

    public void release();
}

