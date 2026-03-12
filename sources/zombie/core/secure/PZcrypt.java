/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.secure;

import org.mindrot.jbcrypt.BCrypt;

public class PZcrypt {
    static String salt = "$2a$12$O/BFHoDFPrfFaNPAACmWpu";

    public static String hash(String originalPassword, boolean checkNull) {
        if (checkNull && originalPassword.isEmpty()) {
            return originalPassword;
        }
        return BCrypt.hashpw(originalPassword, salt);
    }

    public static String hash(String originalPassword) {
        return PZcrypt.hash(originalPassword, true);
    }

    public static String hashSalt(String originalPassword) {
        return BCrypt.hashpw(originalPassword, BCrypt.gensalt(12));
    }

    public static boolean checkHashSalt(String generatedSecuredPasswordHash, String originalPassword) {
        return BCrypt.checkpw(originalPassword, generatedSecuredPasswordHash);
    }
}

