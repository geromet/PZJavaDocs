/*
 * Decompiled with CFR 0.152.
 */
package zombie;

class pzexe {
    pzexe() {
    }

    public static native String findJNI();

    public static void main(String[] stringArray) {
        System.out.println(pzexe.findJNI());
    }

    static {
        if (System.getProperty("sun.arch.data.model").equals("64")) {
            System.out.println("pzexe.java: loading shared library \"pzexe_jni64\"");
            System.loadLibrary("pzexe_jni64");
        } else {
            System.out.println("pzexe.java: loading shared library \"pzexe_jni32\"");
            System.loadLibrary("pzexe_jni32");
        }
    }
}

