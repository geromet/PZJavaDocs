/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import zombie.commands.CommandArgs;

@Target(value={ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface AltCommandArgs {
    public CommandArgs[] value();
}

