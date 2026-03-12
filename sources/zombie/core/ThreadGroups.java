/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

public final class ThreadGroups {
    public static final ThreadGroup Root = new ThreadGroup("PZ");
    public static final ThreadGroup Main = new ThreadGroup(Root, "Main");
    public static final ThreadGroup Workers = new ThreadGroup(Root, "Workers");
    public static final ThreadGroup Network = new ThreadGroup(Root, "Network");
}

