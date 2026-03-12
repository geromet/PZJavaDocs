/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

public enum FoodType {
    BACON("Bacon"),
    BEAN("Bean"),
    BEEF("Beef"),
    BERRY("Berry"),
    BREAD("Bread"),
    CANDY("Candy"),
    CAT_FOOD("CatFood"),
    CHEESE("Cheese"),
    CHOCOLATE("Chocolate"),
    CITRUS("Citrus"),
    COCOA("Cocoa"),
    COFFEE("Coffee"),
    DOG_FOOD("DogFood"),
    DRESSING("Dressing"),
    EGG("Egg"),
    FISH("Fish"),
    FRUITS("Fruits"),
    GAME("Game"),
    GREENS("Greens"),
    HERB("Herb"),
    HOT_PEPPER("HotPepper"),
    INSECT("Insect"),
    JUICE("Juice"),
    MEAT("Meat"),
    MILK("Milk"),
    MUSHROOM("Mushroom"),
    NO_EXPLICIT("NoExplicit"),
    NUT("Nut"),
    OIL("Oil"),
    PASTA("Pasta"),
    POULTRY("Poultry"),
    RICE("Rice"),
    ROE("Roe"),
    SAUSAGE("Sausage"),
    SEAFOOD("Seafood"),
    SEED("Seed"),
    STOCK("Stock"),
    SUGAR("Sugar"),
    TEA("Tea"),
    THICKENER("Thickener"),
    VEGETABLE("Vegetable"),
    VEGETABLES("Vegetables"),
    VENISON("Venison");

    private final String id;

    private FoodType(String id) {
        this.id = id;
    }

    public String toString() {
        return this.id;
    }
}

