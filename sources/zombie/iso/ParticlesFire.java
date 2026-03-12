/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Objects;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL33;
import org.lwjglx.BufferUtils;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.interfaces.ITexture;
import zombie.iso.FireShader;
import zombie.iso.IsoCamera;
import zombie.iso.Particles;
import zombie.iso.ParticlesArray;
import zombie.iso.SmokeShader;
import zombie.iso.weather.ClimateManager;

public final class ParticlesFire
extends Particles {
    int maxParticles = 1000000;
    int maxVortices = 4;
    int particlesDataBuffer;
    ByteBuffer particuleData;
    private final Texture texFireSmoke;
    private final Texture texFlameFire;
    public FireShader effectFire;
    public SmokeShader effectSmoke;
    public Shader effectVape;
    float windX;
    float windY;
    private static ParticlesFire instance;
    private final ParticlesArray<Particle> particles;
    private final ArrayList<Zone> zones;
    private final int intensityFire = 0;
    private final int intensitySmoke = 0;
    private final int intensitySteam = 0;
    private final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);

    public static synchronized ParticlesFire getInstance() {
        if (instance == null) {
            instance = new ParticlesFire();
        }
        return instance;
    }

    public ParticlesFire() {
        this.particles = new ParticlesArray();
        this.zones = new ArrayList();
        this.particuleData = BufferUtils.createByteBuffer(this.maxParticles * 4 * 4);
        this.texFireSmoke = Texture.getSharedTexture("media/textures/FireSmokes.png");
        this.texFlameFire = Texture.getSharedTexture("media/textures/FireFlame.png");
        this.zones.clear();
        float cx = PZMath.fastfloor(IsoCamera.frameState.offX + (float)(IsoCamera.frameState.offscreenWidth / 2));
        float cy = PZMath.fastfloor(IsoCamera.frameState.offY + (float)(IsoCamera.frameState.offscreenHeight / 2));
        this.zones.add(new Zone(this, 10, cx - 30.0f, cy - 10.0f, cx + 30.0f, cy + 10.0f));
        this.zones.add(new Zone(this, 10, cx - 200.0f, cy, 50.0f));
        this.zones.add(new Zone(this, 40, cx + 200.0f, cy, 100.0f));
        this.zones.add(new Zone(this, 60, cx - 150.0f, cy - 300.0f, cx + 250.0f, cy - 300.0f, 10.0f));
        this.zones.add(new Zone(this, 10, cx - 350.0f, cy - 200.0f, cx - 350.0f, cy - 300.0f, 10.0f));
    }

    private void ParticlesProcess() {
        for (int k = 0; k < this.zones.size(); ++k) {
            float r;
            float a;
            Particle p;
            int i;
            Zone z = this.zones.get(k);
            int newParticles = (int)Math.ceil((float)(z.intensity - z.currentParticles) * 0.1f);
            if (z.type == ZoneType.Rectangle) {
                for (i = 0; i < newParticles; ++i) {
                    p = new Particle(this);
                    p.x = Rand.Next(z.x0, z.x1);
                    p.y = Rand.Next(z.y0, z.y1);
                    p.vx = Rand.Next(-3.0f, 3.0f);
                    p.vy = Rand.Next(1.0f, 5.0f);
                    p.tShift = 0.0f;
                    p.id = Rand.Next(-1000000.0f, 1000000.0f);
                    p.zone = z;
                    ++z.currentParticles;
                    this.particles.addParticle(p);
                }
            }
            if (z.type == ZoneType.Circle) {
                for (i = 0; i < newParticles; ++i) {
                    p = new Particle(this);
                    a = Rand.Next(0.0f, (float)Math.PI * 2);
                    r = Rand.Next(0.0f, z.r);
                    p.x = (float)((double)z.x0 + (double)r * Math.cos(a));
                    p.y = (float)((double)z.y0 + (double)r * Math.sin(a));
                    p.vx = Rand.Next(-3.0f, 3.0f);
                    p.vy = Rand.Next(1.0f, 5.0f);
                    p.tShift = 0.0f;
                    p.id = Rand.Next(-1000000.0f, 1000000.0f);
                    p.zone = z;
                    ++z.currentParticles;
                    this.particles.addParticle(p);
                }
            }
            if (z.type == ZoneType.Line) {
                for (i = 0; i < newParticles; ++i) {
                    p = new Particle(this);
                    a = Rand.Next(0.0f, (float)Math.PI * 2);
                    r = Rand.Next(0.0f, z.r);
                    float h = Rand.Next(0.0f, 1.0f);
                    p.x = (float)((double)(z.x0 * h + z.x1 * (1.0f - h)) + (double)r * Math.cos(a));
                    p.y = (float)((double)(z.y0 * h + z.y1 * (1.0f - h)) + (double)r * Math.sin(a));
                    p.vx = Rand.Next(-3.0f, 3.0f);
                    p.vy = Rand.Next(1.0f, 5.0f);
                    p.tShift = 0.0f;
                    p.id = Rand.Next(-1000000.0f, 1000000.0f);
                    p.zone = z;
                    ++z.currentParticles;
                    this.particles.addParticle(p);
                }
            }
            if (newParticles >= 0) continue;
            for (i = 0; i < -newParticles; ++i) {
                --z.currentParticles;
                this.particles.deleteParticle(Rand.Next(0, this.particles.getCount() + 1));
            }
        }
    }

    public FloatBuffer getParametersFire() {
        this.floatBuffer.clear();
        this.floatBuffer.put(this.windX);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(this.windY);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.put(0.0f);
        this.floatBuffer.flip();
        return this.floatBuffer;
    }

    public int getFireShaderID() {
        return this.effectFire.getID();
    }

    public int getSmokeShaderID() {
        return this.effectSmoke.getID();
    }

    public int getVapeShaderID() {
        return this.effectVape.getID();
    }

    public ITexture getFireFlameTexture() {
        return this.texFlameFire;
    }

    public ITexture getFireSmokeTexture() {
        return this.texFireSmoke;
    }

    @Override
    public void reloadShader() {
        RenderThread.invokeOnRenderContext(() -> {
            this.effectFire = new FireShader("fire");
            this.effectSmoke = new SmokeShader("smoke");
            this.effectVape = new Shader("vape");
        });
    }

    @Override
    void createParticleBuffers() {
        this.particlesDataBuffer = funcs.glGenBuffers();
        funcs.glBindBuffer(34962, this.particlesDataBuffer);
        funcs.glBufferData(34962, this.maxParticles * 4 * 4, 35044);
    }

    @Override
    void destroyParticleBuffers() {
        funcs.glDeleteBuffers(this.particlesDataBuffer);
    }

    @Override
    void updateParticleParams() {
        float fireWindAngle = ClimateManager.getInstance().getWindAngleIntensity();
        float fireWindIntensity = ClimateManager.getInstance().getWindIntensity();
        this.windX = (float)Math.sin(fireWindAngle * 6.0f) * fireWindIntensity;
        this.windY = (float)Math.cos(fireWindAngle * 6.0f) * fireWindIntensity;
        this.ParticlesProcess();
        if (this.particles.getNeedToUpdate()) {
            this.particles.defragmentParticle();
            this.particuleData.clear();
            for (int i = 0; i < this.particles.size(); ++i) {
                Particle p = (Particle)this.particles.get(i);
                if (p == null) continue;
                this.particuleData.putFloat(p.x);
                this.particuleData.putFloat(p.y);
                this.particuleData.putFloat(p.id);
                this.particuleData.putFloat((float)i / (float)this.particles.size());
            }
            this.particuleData.flip();
        }
        funcs.glBindBuffer(34962, this.particlesDataBuffer);
        funcs.glBufferData(34962, this.particuleData, 35040);
        GL20.glEnableVertexAttribArray(1);
        funcs.glBindBuffer(34962, this.particlesDataBuffer);
        GL20.glVertexAttribPointer(1, 4, 5126, false, 0, 0L);
        GL33.glVertexAttribDivisor(1, 1);
    }

    @Override
    int getParticleCount() {
        return this.particles.getCount();
    }

    public class Zone {
        ZoneType type;
        int intensity;
        int currentParticles;
        float x0;
        float y0;
        float x1;
        float y1;
        float r;
        float fireIntensity;
        float smokeIntensity;
        float sparksIntensity;
        float vortices;
        float vorticeSpeed;
        float area;
        float temperature;
        float centerX;
        float centerY;
        float centerRp2;
        float currentVorticesCount;

        Zone(ParticlesFire this$0, int intensity, float x, float y, float r) {
            Objects.requireNonNull(this$0);
            this.type = ZoneType.Circle;
            this.intensity = intensity;
            this.currentParticles = 0;
            this.x0 = x;
            this.y0 = y;
            this.r = r;
            this.area = (float)(Math.PI * (double)r * (double)r);
            this.vortices = (float)this.intensity * 0.3f;
            this.vorticeSpeed = 0.5f;
            this.temperature = 2000.0f;
            this.centerX = x;
            this.centerY = y;
            this.centerRp2 = r * r;
        }

        Zone(ParticlesFire this$0, int intensity, float x0, float y0, float x1, float y1) {
            Objects.requireNonNull(this$0);
            this.type = ZoneType.Rectangle;
            this.intensity = intensity;
            this.currentParticles = 0;
            if (x0 < x1) {
                this.x0 = x0;
                this.x1 = x1;
            } else {
                this.x1 = x0;
                this.x0 = x1;
            }
            if (y0 < y1) {
                this.y0 = y0;
                this.y1 = y1;
            } else {
                this.y1 = y0;
                this.y0 = y1;
            }
            this.area = (this.x1 - this.x0) * (this.y1 - this.y0);
            this.vortices = (float)this.intensity * 0.3f;
            this.vorticeSpeed = 0.5f;
            this.temperature = 2000.0f;
            this.centerX = (this.x0 + this.x1) * 0.5f;
            this.centerY = (this.y0 + this.y1) * 0.5f;
            this.centerRp2 = (this.x1 - this.x0) * (this.x1 - this.x0);
        }

        Zone(ParticlesFire this$0, int intensity, float x0, float y0, float x1, float y1, float r) {
            Objects.requireNonNull(this$0);
            this.type = ZoneType.Line;
            this.intensity = intensity;
            this.currentParticles = 0;
            if (x0 < x1) {
                this.x0 = x0;
                this.x1 = x1;
                this.y0 = y0;
                this.y1 = y1;
            } else {
                this.x1 = x0;
                this.x0 = x1;
                this.y1 = y0;
                this.y0 = y1;
            }
            this.r = r;
            this.area = (float)((double)this.r * Math.sqrt(Math.pow(x0 - x1, 2.0) + Math.pow(y0 - y1, 2.0)));
            this.vortices = (float)this.intensity * 0.3f;
            this.vorticeSpeed = 0.5f;
            this.temperature = 2000.0f;
            this.centerX = (this.x0 + this.x1) * 0.5f;
            this.centerY = (this.y0 + this.y1) * 0.5f;
            this.centerRp2 = (this.x1 - this.x0 + r) * (this.x1 - this.x0 + r) * 100.0f;
        }
    }

    static enum ZoneType {
        Rectangle,
        Circle,
        Line;

    }

    public class Particle {
        float id;
        float x;
        float y;
        float tShift;
        float vx;
        float vy;
        Zone zone;

        public Particle(ParticlesFire this$0) {
            Objects.requireNonNull(this$0);
        }
    }

    public class Vortice {
        float x;
        float y;
        float z;
        float size;
        float vx;
        float vy;
        float speed;
        int life;
        int lifeTime;
        Zone zone;

        public Vortice(ParticlesFire this$0) {
            Objects.requireNonNull(this$0);
        }
    }
}

