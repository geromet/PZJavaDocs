/*
 * Decompiled with CFR 0.152.
 */
package zombie.gameStates;

import com.google.common.collect.Lists;
import generation.ScriptFileGenerator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.gameStates.MainScreenState;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.Registry;

public class DevMainScreenState {
    static void main(String[] args2) throws Exception {
        Core.IS_DEV = true;
        Registry<Registry<?>> registry = Registries.REGISTRY;
        ScriptFileGenerator.main(args2);
        MainScreenState.main(args2);
    }

    private static String runGitCommand(String ... extraArgs) throws IOException {
        ArrayList<String> args2 = Lists.newArrayList("git");
        Collections.addAll(args2, extraArgs);
        Process process = new ProcessBuilder(args2).redirectErrorStream(true).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));){
            String line = reader.readLine();
            String string = line == null ? "" : line.trim();
            return string;
        }
    }

    public static String getDevSha() {
        try {
            return DevMainScreenState.runGitCommand("rev-parse", "HEAD");
        }
        catch (IOException e) {
            ExceptionLogger.logException(e, "Unable to get version info, is git on system path & is the game running from the repo?");
            return "missing git";
        }
    }

    public static String getDevVersion() {
        Object name = "Missing Git";
        try {
            String localBranch = DevMainScreenState.runGitCommand("symbolic-ref", "--short", "-q", "HEAD");
            if (localBranch.isEmpty()) {
                name = DevMainScreenState.runGitCommand("name-rev", "--name-only", "HEAD");
                if (((String)name).isEmpty() || "undefined".equals(name) || ((String)name).contains("~")) {
                    name = DevMainScreenState.getDevSha();
                    name = "commit: " + ((String)name).substring(0, Math.min(20, ((String)name).length()));
                } else {
                    name = "branch: " + ((String)name).replaceFirst("^remotes/", "");
                }
            } else {
                name = "branch: " + localBranch;
            }
        }
        catch (IOException e) {
            ExceptionLogger.logException(e, "Unable to get version info, is git on system path & is the game running from the repo?");
        }
        return "Dev @ " + (String)name;
    }
}

