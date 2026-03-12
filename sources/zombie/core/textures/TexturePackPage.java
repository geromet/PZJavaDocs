/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.textures;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import zombie.core.textures.Texture;

public final class TexturePackPage {
    public static HashMap<String, Stack<String>> foundTextures = new HashMap();
    public static final HashMap<String, Texture> subTextureMap = new HashMap();
    public static final HashMap<String, Texture> subTextureMap2 = new HashMap();
    public static final HashMap<String, TexturePackPage> texturePackPageMap = new HashMap();
    public static final HashMap<String, String> TexturePackPageNameMap = new HashMap();
    public final HashMap<String, Texture> subTextures = new HashMap();
    public Texture tex;
    static ByteBuffer sliceBuffer;
    static boolean hasCache;
    static int percent;
    public static int chl1;
    public static int chl2;
    public static int chl3;
    public static int chl4;
    static StringBuilder v;
    public static ArrayList<SubTextureInfo> tempSubTextureInfo;
    public static ArrayList<String> tempFilenameCheck;
    public static boolean ignoreWorldItemTextures;

    public static void LoadDir(String path) throws URISyntaxException {
    }

    public static void searchFolders(File fo) {
    }

    public static Texture getTexture(String tex) {
        if (tex.contains(".png")) {
            return Texture.getSharedTexture(tex);
        }
        if (subTextureMap.containsKey(tex)) {
            return subTextureMap.get(tex);
        }
        return null;
    }

    public static int readInt(InputStream in) throws EOFException, IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        chl1 = ch1;
        chl2 = ch2;
        chl3 = ch3;
        chl4 = ch4;
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return (ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24);
    }

    public static int readInt(ByteBuffer in) throws EOFException, IOException {
        byte ch1 = in.get();
        byte ch2 = in.get();
        byte ch3 = in.get();
        byte ch4 = in.get();
        chl1 = ch1;
        chl2 = ch2;
        chl3 = ch3;
        chl4 = ch4;
        return (ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24);
    }

    public static int readIntByte(InputStream in) throws EOFException, IOException {
        int ch1 = chl2;
        int ch2 = chl3;
        int ch3 = chl4;
        int ch4 = in.read();
        chl1 = ch1;
        chl2 = ch2;
        chl3 = ch3;
        chl4 = ch4;
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }
        return (ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24);
    }

    public static String ReadString(InputStream input) throws IOException {
        v.setLength(0);
        int size = TexturePackPage.readInt(input);
        for (int n = 0; n < size; ++n) {
            v.append((char)input.read());
        }
        return v.toString();
    }

    public void loadFromPackFile(BufferedInputStream input) throws Exception {
        int id;
        boolean mask;
        String name = TexturePackPage.ReadString(input);
        tempFilenameCheck.add(name);
        int numEntries = TexturePackPage.readInt(input);
        boolean bl = mask = TexturePackPage.readInt(input) != 0;
        if (mask) {
            boolean bl2 = false;
        }
        tempSubTextureInfo.clear();
        for (int n = 0; n < numEntries; ++n) {
            String entryName = TexturePackPage.ReadString(input);
            int a = TexturePackPage.readInt(input);
            int b = TexturePackPage.readInt(input);
            int c = TexturePackPage.readInt(input);
            int d = TexturePackPage.readInt(input);
            int e = TexturePackPage.readInt(input);
            int f = TexturePackPage.readInt(input);
            int g = TexturePackPage.readInt(input);
            int h = TexturePackPage.readInt(input);
            if (ignoreWorldItemTextures && entryName.startsWith("WItem_")) continue;
            tempSubTextureInfo.add(new SubTextureInfo(a, b, c, d, e, f, g, h, entryName));
        }
        Texture tex = new Texture(name, input, mask);
        for (int n = 0; n < tempSubTextureInfo.size(); ++n) {
            SubTextureInfo stex = tempSubTextureInfo.get(n);
            Texture st = tex.split(stex.x, stex.y, stex.w, stex.h);
            st.copyMaskRegion(tex, stex.x, stex.y, stex.w, stex.h);
            st.setName(stex.name);
            this.subTextures.put(stex.name, st);
            subTextureMap.put(stex.name, st);
            st.offsetX = stex.ox;
            st.offsetY = stex.oy;
            st.widthOrig = stex.fx;
            st.heightOrig = stex.fy;
        }
        tex.mask = null;
        texturePackPageMap.put(name, this);
        while ((id = TexturePackPage.readIntByte(input)) != -559038737) {
        }
    }

    static {
        v = new StringBuilder(50);
        tempSubTextureInfo = new ArrayList();
        tempFilenameCheck = new ArrayList();
        ignoreWorldItemTextures = true;
    }

    public static class SubTextureInfo {
        public int w;
        public int h;
        public int x;
        public int y;
        public int ox;
        public int oy;
        public int fx;
        public int fy;
        public String name;

        public SubTextureInfo(int x, int y, int w, int h, int ox, int oy, int fx, int fy, String name) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.ox = ox;
            this.oy = oy;
            this.fx = fx;
            this.fy = fy;
            this.name = name;
        }
    }
}

