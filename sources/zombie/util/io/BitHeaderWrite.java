/*
 * Decompiled with CFR 0.152.
 */
package zombie.util.io;

public interface BitHeaderWrite {
    public int getStartPosition();

    public void create();

    public void write();

    public void addFlags(int var1);

    public void addFlags(long var1);

    public boolean hasFlags(int var1);

    public boolean hasFlags(long var1);

    public boolean equals(int var1);

    public boolean equals(long var1);

    public int getLen();

    public void release();
}

