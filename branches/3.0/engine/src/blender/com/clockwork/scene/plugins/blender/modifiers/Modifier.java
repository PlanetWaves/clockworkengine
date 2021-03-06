package com.clockwork.scene.plugins.blender.modifiers;

import com.clockwork.scene.Node;
import com.clockwork.scene.plugins.blender.BlenderContext;
import com.clockwork.scene.plugins.blender.file.Pointer;
import com.clockwork.scene.plugins.blender.file.Structure;

/**
 * This class represents an object's modifier. The modifier object can be varied
 * and the user needs to know what is the type of it for the specified type
 * name. For example "ArmatureModifierData" type specified in blender is
 * represented by AnimData object from engine.
 * 
 * 
 */
public abstract class Modifier {
    public static final String ARRAY_MODIFIER_DATA            = "ArrayModifierData";
    public static final String ARMATURE_MODIFIER_DATA         = "ArmatureModifierData";
    public static final String PARTICLE_MODIFIER_DATA         = "ParticleSystemModifierData";
    public static final String MIRROR_MODIFIER_DATA           = "MirrorModifierData";
    public static final String SUBSURF_MODIFIER_DATA          = "SubsurfModifierData";
    public static final String OBJECT_ANIMATION_MODIFIER_DATA = "ObjectAnimationModifierData";

    /** This variable indicates if the modifier is invalid (true) or not (false). */
    protected boolean          invalid;
    /**
     * A variable that tells if the modifier causes modification. Some modifiers like ArmatureModifier might have no
     * Armature object attached and thus not really modifying the feature. In such cases it is good to know if it is
     * sense to add the modifier to the list of object's modifiers.
     */
    protected boolean          modifying                      = true;

    /**
     * This method applies the modifier to the given node.
     * 
     * @param node
     *            the node that will have modifier applied
     * @param blenderContext
     *            the blender context
     * @return the node with applied modifier
     */
    public abstract Node apply(Node node, BlenderContext blenderContext);

    /**
     * Determines if the modifier can be applied multiple times over one mesh.
     * At this moment only armature and object animation modifiers cannot be
     * applied multiple times.
     * 
     * @param modifierType
     *            the type name of the modifier
     * @return true if the modifier can be applied many times and
     *         false otherwise
     */
    public static boolean canBeAppliedMultipleTimes(String modifierType) {
        return !(ARMATURE_MODIFIER_DATA.equals(modifierType) || OBJECT_ANIMATION_MODIFIER_DATA.equals(modifierType));
    }

    protected boolean validate(Structure modifierStructure, BlenderContext blenderContext) {
        Structure modifierData = (Structure) modifierStructure.getFieldValue("modifier");
        Pointer pError = (Pointer) modifierData.getFieldValue("error");
        invalid = pError.isNotNull();
        return !invalid;
    }

    /**
     * @return true if the modifier causes feature's modification or false if not
     */
    public boolean isModifying() {
        return modifying;
    }
}
