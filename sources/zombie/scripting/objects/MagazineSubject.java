/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class MagazineSubject {
    public static final MagazineSubject ART = MagazineSubject.registerBase("Art");
    public static final MagazineSubject BUSINESS = MagazineSubject.registerBase("Business");
    public static final MagazineSubject CARS = MagazineSubject.registerBase("Cars");
    public static final MagazineSubject CHILDS = MagazineSubject.registerBase("Childs");
    public static final MagazineSubject CINEMA = MagazineSubject.registerBase("Cinema");
    public static final MagazineSubject CRIME = MagazineSubject.registerBase("Crime");
    public static final MagazineSubject FASHION = MagazineSubject.registerBase("Fashion");
    public static final MagazineSubject FIREARM = MagazineSubject.registerBase("Firearm");
    public static final MagazineSubject GAMING = MagazineSubject.registerBase("Gaming");
    public static final MagazineSubject GOLF = MagazineSubject.registerBase("Golf");
    public static final MagazineSubject HEALTH = MagazineSubject.registerBase("Health");
    public static final MagazineSubject HOBBY = MagazineSubject.registerBase("Hobby");
    public static final MagazineSubject HORROR = MagazineSubject.registerBase("Horror");
    public static final MagazineSubject HUMOR = MagazineSubject.registerBase("Humor");
    public static final MagazineSubject MILITARY = MagazineSubject.registerBase("Military");
    public static final MagazineSubject MUSIC = MagazineSubject.registerBase("Music");
    public static final MagazineSubject OUTDOORS = MagazineSubject.registerBase("Outdoors");
    public static final MagazineSubject POLICE = MagazineSubject.registerBase("Police");
    public static final MagazineSubject POPULAR = MagazineSubject.registerBase("Popular");
    public static final MagazineSubject RICH = MagazineSubject.registerBase("Rich");
    public static final MagazineSubject SCIENCE = MagazineSubject.registerBase("Science");
    public static final MagazineSubject SPORTS = MagazineSubject.registerBase("Sports");
    public static final MagazineSubject TECH = MagazineSubject.registerBase("Tech");
    public static final MagazineSubject TEENS = MagazineSubject.registerBase("Teens");
    private final String key;

    private MagazineSubject(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    public static MagazineSubject get(ResourceLocation id) {
        return Registries.MAGAZINE_SUBJECT.get(id);
    }

    public String toString() {
        return Registries.MAGAZINE_SUBJECT.getLocation(this).toString();
    }

    public static MagazineSubject register(String id) {
        return MagazineSubject.register(false, id);
    }

    private static MagazineSubject registerBase(String id) {
        return MagazineSubject.register(true, id);
    }

    private static MagazineSubject register(boolean allowDefaultNamespace, String id) {
        return Registries.MAGAZINE_SUBJECT.register(RegistryReset.createLocation(id, allowDefaultNamespace), new MagazineSubject(id));
    }
}

