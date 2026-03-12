/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import zombie.commands.CommandNames;

@Target(value={ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
@Repeatable(value=CommandNames.class)
public @interface CommandName {
    public String name();
}

