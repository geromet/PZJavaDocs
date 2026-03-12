/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjglx.BufferUtils;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.Vector3;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.model.VertexPositionNormalTangentTextureSkin;
import zombie.core.textures.Texture;
import zombie.creative.creativerects.OpenSimplexNoise;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;

public final class HeightTerrain {
    private final ByteBuffer buffer;
    public VertexBufferObject vb;
    public static float isoAngle = 62.65607f;
    public static float scale = 0.047085002f;
    OpenSimplexNoise noise = new OpenSimplexNoise(Rand.Next(10000000));
    static float[] lightAmbient = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    static float[] lightDiffuse = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    static float[] lightPosition = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    static float[] specular = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    static float[] shininess = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    static float[] emission = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    static float[] ambient = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    static float[] diffuse = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    static ByteBuffer temp = ByteBuffer.allocateDirect(16);

    public HeightTerrain(int widthTiles, int heightTiles) {
        VertexPositionNormalTangentTextureSkin vert;
        int y;
        int tilesX = widthTiles;
        int tilesY = heightTiles;
        ArrayList<VertexPositionNormalTangentTextureSkin> verts = new ArrayList<VertexPositionNormalTangentTextureSkin>();
        int vertexCount = tilesX * tilesY;
        int xVerts = tilesX;
        int yVerts = tilesY;
        ArrayList<Integer> indices = new ArrayList<Integer>();
        Vector2 size = new Vector2(2.0f, 0.0f);
        for (int x = 0; x < xVerts; ++x) {
            for (y = 0; y < yVerts; ++y) {
                float noise = (float)this.calc(x, y);
                noise *= 1.0f;
                vert = new VertexPositionNormalTangentTextureSkin();
                vert.position = new Vector3();
                vert.position.set(-x, (noise += 1.0f) * 30.0f, -y);
                vert.normal = new Vector3();
                vert.normal.set(0.0f, 1.0f, 0.0f);
                vert.normal.normalize();
                vert.textureCoordinates = new Vector2((float)x / (float)(xVerts - 1) * 16.0f, (float)y / (float)(yVerts - 1) * 16.0f);
                verts.add(vert);
            }
        }
        int n = 0;
        for (int x = 0; x < xVerts; ++x) {
            for (int y2 = 0; y2 < yVerts; ++y2) {
                vert = (VertexPositionNormalTangentTextureSkin)verts.get(n);
                Vector3 va = new Vector3();
                Vector3 vb = new Vector3();
                float noises21 = (float)this.calc(x + 1, y2);
                noises21 *= 1.0f;
                noises21 += 1.0f;
                float noises01 = (float)this.calc(x - 1, y2);
                noises01 *= 1.0f;
                noises01 += 1.0f;
                float noises12 = (float)this.calc(x, y2 + 1);
                noises12 *= 1.0f;
                float noises10 = (float)this.calc(x, y2 - 1);
                noises10 *= 1.0f;
                float s21 = noises21 * 700.0f;
                float s01 = noises01 * 700.0f;
                float s12 = (noises12 += 1.0f) * 700.0f;
                float s10 = (noises10 += 1.0f) * 700.0f;
                va.set(size.x, size.y, s21 - s01);
                vb.set(size.y, size.x, s12 - s10);
                va.normalize();
                vb.normalize();
                Vector3 cross = va.cross(vb);
                vert.normal.x(cross.x());
                vert.normal.y(cross.z());
                vert.normal.z(cross.y());
                vert.normal.normalize();
                System.out.println(vert.normal.x() + " , " + vert.normal.y() + ", " + vert.normal.z());
                vert.normal.normalize();
                ++n;
            }
        }
        n = 0;
        for (y = 0; y < yVerts - 1; ++y) {
            int x;
            if ((y & 1) == 0) {
                for (x = 0; x < xVerts; ++x) {
                    indices.add(x + (y + 1) * xVerts);
                    indices.add(x + y * xVerts);
                    ++n;
                    ++n;
                }
                continue;
            }
            for (x = xVerts - 1; x > 0; --x) {
                indices.add(x - 1 + y * xVerts);
                indices.add(x + (y + 1) * xVerts);
                ++n;
                ++n;
            }
        }
        if ((xVerts & 1) > 0 && yVerts > 2) {
            indices.add((yVerts - 1) * xVerts);
            ++n;
        }
        this.vb = new VertexBufferObject();
        ByteBuffer vertBuffer = BufferUtils.createByteBuffer(verts.size() * 36);
        for (int i = 0; i < verts.size(); ++i) {
            vert = (VertexPositionNormalTangentTextureSkin)verts.get(i);
            vertBuffer.putFloat(vert.position.x());
            vertBuffer.putFloat(vert.position.y());
            vertBuffer.putFloat(vert.position.z());
            vertBuffer.putFloat(vert.normal.x());
            vertBuffer.putFloat(vert.normal.y());
            vertBuffer.putFloat(vert.normal.z());
            int col = -1;
            vertBuffer.putInt(-1);
            vertBuffer.putFloat(vert.textureCoordinates.x);
            vertBuffer.putFloat(vert.textureCoordinates.y);
        }
        vertBuffer.flip();
        int[] ind = new int[indices.size()];
        for (int i = 0; i < indices.size(); ++i) {
            Integer indice = (Integer)indices.get(indices.size() - 1 - i);
            ind[i] = indice;
        }
        this.vb.handle = this.vb.LoadSoftwareVBO(vertBuffer, this.vb.handle, ind);
        this.buffer = vertBuffer;
    }

    double calcTerrain(float x, float y) {
        double res = this.noise.eval((x *= 10.0f) / 900.0f, (y *= 10.0f) / 600.0f, 0.0);
        res += this.noise.eval(x / 600.0f, y / 600.0f, 0.0) / 4.0;
        res += (this.noise.eval(x / 300.0f, y / 300.0f, 0.0) + 1.0) / 8.0;
        res += (this.noise.eval(x / 150.0f, y / 150.0f, 0.0) + 1.0) / 16.0;
        return res += (this.noise.eval(x / 75.0f, y / 75.0f, 0.0) + 1.0) / 32.0;
    }

    double calc(float x, float y) {
        return this.calcTerrain(x, y);
    }

    public void pushView(int ox, int oy, int oz) {
        GL11.glDepthMask(false);
        GL11.glMatrixMode(5889);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        double screenWidth = (float)Math.abs(Core.getInstance().getOffscreenWidth(0)) / 1920.0f;
        double screenHeight = (float)Math.abs(Core.getInstance().getOffscreenHeight(0)) / 1080.0f;
        GL11.glLoadIdentity();
        GL11.glOrtho(-screenWidth / 2.0, screenWidth / 2.0, -screenHeight / 2.0, screenHeight / 2.0, -10.0, 10.0);
        GL11.glMatrixMode(5888);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glScaled(scale, scale, scale);
        GL11.glRotatef(isoAngle, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(135.0f, 0.0f, 1.0f, 0.0f);
        GL11.glTranslated(IsoWorld.instance.currentCell.chunkMap[0].getWidthInTiles() / 2, 0.0, IsoWorld.instance.currentCell.chunkMap[0].getWidthInTiles() / 2);
        GL11.glDepthRange(-100.0, 100.0);
    }

    public void popView() {
        GL11.glEnable(3008);
        GL11.glDepthFunc(519);
        GL11.glDepthMask(false);
        GL11.glMatrixMode(5889);
        GL11.glPopMatrix();
        GL11.glMatrixMode(5888);
        GL11.glPopMatrix();
    }

    public void render() {
        GL11.glPushClientAttrib(-1);
        GL11.glPushAttrib(1048575);
        GL11.glDisable(2884);
        GL11.glEnable(2929);
        GL11.glDepthFunc(519);
        GL11.glColorMask(true, true, true, true);
        GL11.glAlphaFunc(519, 0.0f);
        GL11.glDepthFunc(519);
        GL11.glDepthRange(-10.0, 10.0);
        GL11.glEnable(2903);
        GL11.glEnable(2896);
        GL11.glEnable(16384);
        GL11.glEnable(16385);
        GL11.glEnable(2929);
        GL11.glDisable(3008);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(3008);
        GL11.glAlphaFunc(519, 0.0f);
        GL11.glDisable(3089);
        this.doLighting();
        GL11.glDisable(2929);
        GL11.glEnable(3553);
        GL11.glBlendFunc(770, 771);
        GL11.glCullFace(1029);
        this.pushView(IsoPlayer.getInstance().getCurrentSquare().getChunk().wx / 30 * 300, IsoPlayer.getInstance().getCurrentSquare().getChunk().wy / 30 * 300, 0);
        Texture.getSharedTexture("media/textures/grass.png").bind();
        this.vb.DrawStrip(null);
        this.popView();
        GL11.glEnable(3042);
        GL11.glDisable(3008);
        GL11.glDisable(2929);
        GL11.glEnable(6144);
        if (PerformanceSettings.modelLighting) {
            GL11.glDisable(2903);
            GL11.glDisable(2896);
            GL11.glDisable(16384);
            GL11.glDisable(16385);
        }
        GL11.glDepthRange(0.0, 100.0);
        SpriteRenderer.ringBuffer.restoreVbos = true;
        GL11.glEnable(2929);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(3008);
        GL11.glAlphaFunc(516, 0.0f);
        GL11.glEnable(3553);
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
    }

    private void doLighting() {
        temp.order(ByteOrder.nativeOrder());
        temp.clear();
        GL11.glColorMaterial(1032, 5634);
        GL11.glDisable(2903);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2896);
        GL11.glEnable(16384);
        GL11.glDisable(16385);
        HeightTerrain.lightAmbient[0] = 0.7f;
        HeightTerrain.lightAmbient[1] = 0.7f;
        HeightTerrain.lightAmbient[2] = 0.7f;
        HeightTerrain.lightAmbient[3] = 0.5f;
        HeightTerrain.lightDiffuse[0] = 0.5f;
        HeightTerrain.lightDiffuse[1] = 0.5f;
        HeightTerrain.lightDiffuse[2] = 0.5f;
        HeightTerrain.lightDiffuse[3] = 1.0f;
        Vector3 v = new Vector3(1.0f, 1.0f, 1.0f);
        v.normalize();
        HeightTerrain.lightPosition[0] = -v.x();
        HeightTerrain.lightPosition[1] = v.y();
        HeightTerrain.lightPosition[2] = -v.z();
        HeightTerrain.lightPosition[3] = 0.0f;
        GL11.glLightfv(16384, 4608, temp.asFloatBuffer().put(lightAmbient).flip());
        GL11.glLightfv(16384, 4609, temp.asFloatBuffer().put(lightDiffuse).flip());
        GL11.glLightfv(16384, 4611, temp.asFloatBuffer().put(lightPosition).flip());
        GL11.glLightf(16384, 4615, 0.0f);
        GL11.glLightf(16384, 4616, 0.0f);
        GL11.glLightf(16384, 4617, 0.0f);
        HeightTerrain.specular[0] = 0.0f;
        HeightTerrain.specular[1] = 0.0f;
        HeightTerrain.specular[2] = 0.0f;
        HeightTerrain.specular[3] = 0.0f;
        GL11.glMaterialfv(1032, 4610, temp.asFloatBuffer().put(specular).flip());
        GL11.glMaterialfv(1032, 5633, temp.asFloatBuffer().put(specular).flip());
        GL11.glMaterialfv(1032, 5632, temp.asFloatBuffer().put(specular).flip());
        HeightTerrain.ambient[0] = 0.6f;
        HeightTerrain.ambient[1] = 0.6f;
        HeightTerrain.ambient[2] = 0.6f;
        HeightTerrain.ambient[3] = 1.0f;
        HeightTerrain.diffuse[0] = 0.6f;
        HeightTerrain.diffuse[1] = 0.6f;
        HeightTerrain.diffuse[2] = 0.6f;
        HeightTerrain.diffuse[3] = 0.6f;
        GL11.glMaterialfv(1032, 4608, temp.asFloatBuffer().put(ambient).flip());
        GL11.glMaterialfv(1032, 4609, temp.asFloatBuffer().put(diffuse).flip());
    }
}

