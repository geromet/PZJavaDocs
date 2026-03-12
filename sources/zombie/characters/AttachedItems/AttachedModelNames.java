/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.AttachedItems;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.AttachedItems.AttachedItem;
import zombie.characters.AttachedItems.AttachedItems;
import zombie.characters.AttachedItems.AttachedLocationGroup;
import zombie.characters.AttachedItems.AttachedModelName;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.scripting.objects.ModelWeaponPart;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class AttachedModelNames {
    protected AttachedLocationGroup group;
    protected final ArrayList<AttachedModelName> models = new ArrayList();

    AttachedLocationGroup getGroup() {
        return this.group;
    }

    public void copyFrom(AttachedModelNames other) {
        this.models.clear();
        for (int i = 0; i < other.models.size(); ++i) {
            AttachedModelName amn = other.models.get(i);
            this.models.add(new AttachedModelName(amn));
        }
    }

    public void initFrom(AttachedItems attachedItems) {
        if (attachedItems == null) {
            this.group = null;
            this.models.clear();
            return;
        }
        this.group = attachedItems.getGroup();
        this.models.clear();
        for (int i = 0; i < attachedItems.size(); ++i) {
            ArrayList<ModelWeaponPart> modelWeaponParts;
            AttachedItem attachedItem = attachedItems.get(i);
            String modelName = attachedItem.getItem().getStaticModelException();
            if (StringUtils.isNullOrWhitespace(modelName)) continue;
            String attachmentName = this.group.getLocation(attachedItem.getLocation()).getAttachmentName();
            HandWeapon weapon = Type.tryCastTo(attachedItem.getItem(), HandWeapon.class);
            float bloodLevel = weapon == null ? 0.0f : weapon.getBloodLevel();
            AttachedModelName amn = new AttachedModelName(attachmentName, modelName, bloodLevel);
            this.models.add(amn);
            if (weapon == null || (modelWeaponParts = weapon.getModelWeaponPart()) == null) continue;
            List<WeaponPart> weaponParts = weapon.getAllWeaponParts();
            block1: for (int j = 0; j < weaponParts.size(); ++j) {
                WeaponPart part = weaponParts.get(j);
                for (int k = 0; k < modelWeaponParts.size(); ++k) {
                    ModelWeaponPart mwp = modelWeaponParts.get(k);
                    if (!part.getFullType().equals(mwp.partType)) continue;
                    AttachedModelName amn2 = new AttachedModelName(mwp.attachmentNameSelf, mwp.attachmentParent, mwp.modelName, 0.0f);
                    amn.addChild(amn2);
                    continue block1;
                }
            }
        }
    }

    public int size() {
        return this.models.size();
    }

    public AttachedModelName get(int index) {
        return this.models.get(index);
    }

    public void clear() {
        this.models.clear();
    }
}

