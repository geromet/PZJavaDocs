/*
 * Decompiled with CFR 0.152.
 */
package zombie.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import zombie.commands.AltCommandArgs;

@Target(value={ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
@Repeatable(value=AltCommandArgs.class)
public @interface CommandArgs {
    public static final String DEFAULT_OPTIONAL_ARGUMENT = "no value";

    public String[] required() default {};

    public String optional() default "no value";

    public String argName() default "NO_ARGUMENT_NAME";

    public boolean varArgs() default false;
}

