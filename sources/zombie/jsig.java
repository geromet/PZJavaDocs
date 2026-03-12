/*
 * Decompiled with CFR 0.152.
 */
package zombie;

class jsig {
    jsig() {
    }

    public static native String findJSIG();

    public static void main(String[] stringArray) {
        System.out.println(jsig.findJSIG());
    }

    static {
        if (System.getProperty("sun.arch.data.model").equals("64")) {
            System.loadLibrary("pzexe_jni64");
        } else {
            System.loadLibrary("pzexe_jni32");
        }
    }
}

