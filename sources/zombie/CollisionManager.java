/*
 * Decompiled with CFR 0.152.
 */
package zombie;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import zombie.characters.AnimalFootstepManager;
import zombie.characters.AnimalVocalsManager;
import zombie.characters.IsoSurvivor;
import zombie.characters.ZombieFootstepManager;
import zombie.characters.ZombieThumpManager;
import zombie.characters.ZombieVocalsManager;
import zombie.characters.animals.AnimalPopulationManager;
import zombie.core.collision.Polygon;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.iso.FishSplashSoundManager;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoPushableObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;

public final class CollisionManager {
    static Vector2 temp = new Vector2();
    static Vector2 axis = new Vector2();
    static Polygon polygonA = new Polygon();
    static Polygon polygonB = new Polygon();
    float minA;
    float minB;
    float maxA;
    float maxB;
    PolygonCollisionResult result = new PolygonCollisionResult(this);
    public ArrayList<Contact> contactMap = new ArrayList();
    Long[] longArray = new Long[1000];
    Stack<Contact> contacts = new Stack();
    public static final CollisionManager instance = new CollisionManager();

    private void ProjectPolygonA(Vector2 axis, Polygon polygon) {
        float d;
        this.minA = d = axis.dot(polygon.points.get(0));
        this.maxA = d;
        for (int i = 0; i < polygon.points.size(); ++i) {
            d = polygon.points.get(i).dot(axis);
            if (d < this.minA) {
                this.minA = d;
                continue;
            }
            if (!(d > this.maxA)) continue;
            this.maxA = d;
        }
    }

    private void ProjectPolygonB(Vector2 axis, Polygon polygon) {
        float d;
        this.minB = d = axis.dot(polygon.points.get(0));
        this.maxB = d;
        for (int i = 0; i < polygon.points.size(); ++i) {
            d = polygon.points.get(i).dot(axis);
            if (d < this.minB) {
                this.minB = d;
                continue;
            }
            if (!(d > this.maxB)) continue;
            this.maxB = d;
        }
    }

    public PolygonCollisionResult PolygonCollision(Vector2 velocity) {
        this.result.intersect = true;
        this.result.willIntersect = true;
        this.result.minimumTranslationVector.x = 0.0f;
        this.result.minimumTranslationVector.y = 0.0f;
        int edgeCountA = CollisionManager.polygonA.edges.size();
        int edgeCountB = CollisionManager.polygonB.edges.size();
        float minIntervalDistance = Float.POSITIVE_INFINITY;
        Vector2 translationAxis = new Vector2();
        for (int edgeIndex = 0; edgeIndex < edgeCountA + edgeCountB; ++edgeIndex) {
            float velocityProjection;
            Vector2 edge = edgeIndex < edgeCountA ? CollisionManager.polygonA.edges.get(edgeIndex) : CollisionManager.polygonB.edges.get(edgeIndex - edgeCountA);
            CollisionManager.axis.x = -edge.y;
            CollisionManager.axis.y = edge.x;
            axis.normalize();
            this.minA = 0.0f;
            this.minB = 0.0f;
            this.maxA = 0.0f;
            this.maxB = 0.0f;
            this.ProjectPolygonA(axis, polygonA);
            this.ProjectPolygonB(axis, polygonB);
            if (this.IntervalDistance(this.minA, this.maxA, this.minB, this.maxB) > 0.0f) {
                this.result.intersect = false;
            }
            if ((velocityProjection = axis.dot(velocity)) < 0.0f) {
                this.minA += velocityProjection;
            } else {
                this.maxA += velocityProjection;
            }
            float intervalDistance = this.IntervalDistance(this.minA, this.maxA, this.minB, this.maxB);
            if (intervalDistance > 0.0f) {
                this.result.willIntersect = false;
            }
            if (!this.result.intersect && !this.result.willIntersect) break;
            if (!((intervalDistance = Math.abs(intervalDistance)) < minIntervalDistance)) continue;
            minIntervalDistance = intervalDistance;
            translationAxis.x = CollisionManager.axis.x;
            translationAxis.y = CollisionManager.axis.y;
            CollisionManager.temp.x = CollisionManager.polygonA.Center().x - CollisionManager.polygonB.Center().x;
            CollisionManager.temp.y = CollisionManager.polygonA.Center().y - CollisionManager.polygonB.Center().y;
            if (!(temp.dot(translationAxis) < 0.0f)) continue;
            translationAxis.x = -translationAxis.x;
            translationAxis.y = -translationAxis.y;
        }
        if (this.result.willIntersect) {
            this.result.minimumTranslationVector.x = translationAxis.x * minIntervalDistance;
            this.result.minimumTranslationVector.y = translationAxis.y * minIntervalDistance;
        }
        return this.result;
    }

    public float IntervalDistance(float minA, float maxA, float minB, float maxB) {
        if (minA < minB) {
            return minB - maxA;
        }
        return minA - maxB;
    }

    public void initUpdate() {
        int n;
        if (this.longArray[0] == null) {
            for (n = 0; n < this.longArray.length; ++n) {
                this.longArray[n] = 0L;
            }
        }
        for (n = 0; n < this.contactMap.size(); ++n) {
            this.contactMap.get((int)n).a = null;
            this.contactMap.get((int)n).b = null;
            this.contacts.push(this.contactMap.get(n));
        }
        this.contactMap.clear();
    }

    public void AddContact(IsoMovingObject a, IsoMovingObject b) {
        if ((a instanceof IsoSurvivor || b instanceof IsoSurvivor) && (a instanceof IsoPushableObject || b instanceof IsoPushableObject)) {
            return;
        }
        if (a.getID() < b.getID()) {
            this.contactMap.add(this.contact(a, b));
        }
    }

    Contact contact(IsoMovingObject a, IsoMovingObject b) {
        if (this.contacts.isEmpty()) {
            for (int n = 0; n < 50; ++n) {
                this.contacts.push(new Contact(this, null, null));
            }
        }
        Contact c = this.contacts.pop();
        c.a = a;
        c.b = b;
        return c;
    }

    public void ResolveContacts() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.profile_ResolveContacts.profile();){
            this.resolveContactsInternal();
        }
    }

    /*
     * Unable to fully structure code
     */
    private void resolveContactsInternal() {
        vel = l_ResolveContacts.vel;
        vel2 = l_ResolveContacts.vel2;
        pushables = l_ResolveContacts.pushables;
        pol = IsoWorld.instance.currentCell.getPushableObjectList();
        size = pol.size();
        for (i = 0; i < size; ++i) {
            o = (IsoPushableObject)pol.get(i);
            if (o.getImpulsex() == 0.0f && o.getImpulsey() == 0.0f) continue;
            if (o.connectList != null) {
                pushables.add(o);
                continue;
            }
            o.setNextX(o.getNextX() + o.getImpulsex());
            o.setNextY(o.getNextY() + o.getImpulsey());
            o.setImpulsex(o.getNextX() - o.getX());
            o.setImpulsey(o.getNextY() - o.getY());
            o.setNextX(o.getX());
            o.setNextY(o.getY());
        }
        numPushables = pushables.size();
        for (n = 0; n < numPushables; ++n) {
            p = pushables.get(n);
            impulseTotx = 0.0f;
            impulseToty = 0.0f;
            for (m = 0; m < p.connectList.size(); ++m) {
                impulseTotx += p.connectList.get(m).getImpulsex();
                impulseToty += p.connectList.get(m).getImpulsey();
            }
            impulseTotx /= (float)p.connectList.size();
            impulseToty /= (float)p.connectList.size();
            for (m = 0; m < p.connectList.size(); ++m) {
                p.connectList.get(m).setImpulsex(impulseTotx);
                p.connectList.get(m).setImpulsey(impulseToty);
                inof = pushables.indexOf(p.connectList.get(m));
                pushables.remove(p.connectList.get(m));
                if (inof > n) continue;
                --n;
            }
            if (n >= 0) continue;
            n = 0;
        }
        pushables.clear();
        numContacts = this.contactMap.size();
        for (i = 0; i < numContacts; ++i) {
            c = this.contactMap.get(i);
            if (Math.abs(c.a.getZ() - c.b.getZ()) > 0.3f) continue;
            vel.x = c.a.getNextX() - c.a.getX();
            vel.y = c.a.getNextY() - c.a.getY();
            vel2.x = c.b.getNextX() - c.b.getX();
            vel2.y = c.b.getNextY() - c.b.getY();
            if (vel.x == 0.0f && vel.y == 0.0f && vel2.x == 0.0f && vel2.y == 0.0f && c.a.getImpulsex() == 0.0f && c.a.getImpulsey() == 0.0f && c.b.getImpulsex() == 0.0f && c.b.getImpulsey() == 0.0f) continue;
            ax1 = c.a.getX() - c.a.getWidth();
            ax2 = c.a.getX() + c.a.getWidth();
            ay1 = c.a.getY() - c.a.getWidth();
            ay2 = c.a.getY() + c.a.getWidth();
            bx1 = c.b.getX() - c.b.getWidth();
            bx2 = c.b.getX() + c.b.getWidth();
            by1 = c.b.getY() - c.b.getWidth();
            by2 = c.b.getY() + c.b.getWidth();
            CollisionManager.polygonA.Set(ax1, ay1, ax2, ay2);
            CollisionManager.polygonB.Set(bx1, by1, bx2, by2);
            result = this.PolygonCollision(vel);
            if (!result.willIntersect) continue;
            c.a.collideWith(c.b);
            c.b.collideWith(c.a);
            weightdelta = 1.0f - c.a.getWeight(result.minimumTranslationVector.x, result.minimumTranslationVector.y) / (c.a.getWeight(result.minimumTranslationVector.x, result.minimumTranslationVector.y) + c.b.getWeight(result.minimumTranslationVector.x, result.minimumTranslationVector.y));
            var24_35 = c.a;
            if (!(var24_35 instanceof IsoPushableObject)) ** GOTO lbl-1000
            object = (IsoPushableObject)var24_35;
            var24_35 = c.b;
            if (var24_35 instanceof IsoSurvivor) {
                survivor = (IsoSurvivor)var24_35;
                survivor.collidedWithPushable = true;
                survivor.collidePushable = object;
            } else if ((var24_35 = c.b) instanceof IsoPushableObject) {
                isoPushableObject = (IsoPushableObject)var24_35;
                var24_35 = c.a;
                if (var24_35 instanceof IsoSurvivor) {
                    isoSurvivor = (IsoSurvivor)var24_35;
                    isoSurvivor.collidedWithPushable = true;
                    isoSurvivor.collidePushable = isoPushableObject;
                }
            }
            survivor = c.a;
            if (survivor instanceof IsoPushableObject) {
                pushableObject = (IsoPushableObject)survivor;
                connectListA = pushableObject.connectList;
                if (connectListA != null) {
                    connectListSize = connectListA.size();
                    for (j = 0; j < connectListSize; ++j) {
                        p = (IsoPushableObject)connectListA.get(j);
                        p.setImpulsex(p.getImpulsex() + result.minimumTranslationVector.x * weightdelta);
                        p.setImpulsey(p.getImpulsey() + result.minimumTranslationVector.y * weightdelta);
                    }
                }
            } else {
                c.a.setImpulsex(c.a.getImpulsex() + result.minimumTranslationVector.x * weightdelta);
                c.a.setImpulsey(c.a.getImpulsey() + result.minimumTranslationVector.y * weightdelta);
            }
            connectListA = c.b;
            if (connectListA instanceof IsoPushableObject) {
                isoPushableObject = (IsoPushableObject)connectListA;
                connectListB = isoPushableObject.connectList;
                if (connectListB == null) continue;
                connectListSize = connectListB.size();
                for (j = 0; j < connectListSize; ++j) {
                    p = connectListB.get(j);
                    p.setImpulsex(p.getImpulsex() - result.minimumTranslationVector.x * (1.0f - weightdelta));
                    p.setImpulsey(p.getImpulsey() - result.minimumTranslationVector.y * (1.0f - weightdelta));
                }
                continue;
            }
            c.b.setImpulsex(c.b.getImpulsex() - result.minimumTranslationVector.x * (1.0f - weightdelta));
            c.b.setImpulsey(c.b.getImpulsey() - result.minimumTranslationVector.y * (1.0f - weightdelta));
        }
        AnimalPopulationManager.getInstance().update();
        AnimalVocalsManager.instance.update();
        AnimalFootstepManager.instance.update();
        IsoMovingObject.treeSoundMgr.update();
        FishSplashSoundManager.instance.update();
        ZombieFootstepManager.instance.update();
        ZombieThumpManager.instance.update();
        ZombieVocalsManager.instance.update();
    }

    public class PolygonCollisionResult {
        public boolean willIntersect;
        public boolean intersect;
        public Vector2 minimumTranslationVector;

        public PolygonCollisionResult(CollisionManager this$0) {
            Objects.requireNonNull(this$0);
            this.minimumTranslationVector = new Vector2();
        }
    }

    public class Contact {
        public IsoMovingObject a;
        public IsoMovingObject b;

        public Contact(CollisionManager this$0, IsoMovingObject a, IsoMovingObject b) {
            Objects.requireNonNull(this$0);
            this.a = a;
            this.b = b;
        }
    }

    private static class s_performance {
        static final PerformanceProfileProbe profile_ResolveContacts = new PerformanceProfileProbe("CollisionManager.ResolveContacts");
        static final PerformanceProfileProbe profile_MovingObjectPostUpdate = new PerformanceProfileProbe("IsoMovingObject.postupdate");

        private s_performance() {
        }
    }

    private static class l_ResolveContacts {
        static final Vector2 vel = new Vector2();
        static final Vector2 vel2 = new Vector2();
        static final List<IsoPushableObject> pushables = new ArrayList<IsoPushableObject>();
        static IsoMovingObject[] objectListInvoking = new IsoMovingObject[1024];

        private l_ResolveContacts() {
        }
    }
}

