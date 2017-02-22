package com.clockwork.scene.plugins.blender.constraints.definitions;

import com.clockwork.animation.Bone;
import com.clockwork.math.Transform;
import com.clockwork.math.Vector3f;
import com.clockwork.scene.plugins.blender.BlenderContext;
import com.clockwork.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Loc limit' constraint type in blender.
 * 
 * 
 */
/* package */class ConstraintDefinitionLocLimit extends ConstraintDefinition {
    private static final int LIMIT_XMIN = 0x01;
    private static final int LIMIT_XMAX = 0x02;
    private static final int LIMIT_YMIN = 0x04;
    private static final int LIMIT_YMAX = 0x08;
    private static final int LIMIT_ZMIN = 0x10;
    private static final int LIMIT_ZMAX = 0x20;

    protected float[][]      limits     = new float[3][2];

    public ConstraintDefinitionLocLimit(Structure constraintData, Long ownerOMA, BlenderContext blenderContext) {
        super(constraintData, ownerOMA, blenderContext);
        if (blenderContext.getBlenderKey().isFixUpAxis()) {
            limits[0][0] = ((Number) constraintData.getFieldValue("xmin")).floatValue();
            limits[0][1] = ((Number) constraintData.getFieldValue("xmax")).floatValue();
            limits[2][0] = -((Number) constraintData.getFieldValue("ymin")).floatValue();
            limits[2][1] = -((Number) constraintData.getFieldValue("ymax")).floatValue();
            limits[1][0] = ((Number) constraintData.getFieldValue("zmin")).floatValue();
            limits[1][1] = ((Number) constraintData.getFieldValue("zmax")).floatValue();

            // swapping Y and X limits flag in the bitwise flag
            int ymin = flag & LIMIT_YMIN;
            int ymax = flag & LIMIT_YMAX;
            int zmin = flag & LIMIT_ZMIN;
            int zmax = flag & LIMIT_ZMAX;
            flag &= LIMIT_XMIN | LIMIT_XMAX;// clear the other flags to swap
                                            // them
            flag |= ymin << 2;
            flag |= ymax << 2;
            flag |= zmin >> 2;
            flag |= zmax >> 2;
        } else {
            limits[0][0] = ((Number) constraintData.getFieldValue("xmin")).floatValue();
            limits[0][1] = ((Number) constraintData.getFieldValue("xmax")).floatValue();
            limits[1][0] = ((Number) constraintData.getFieldValue("ymin")).floatValue();
            limits[1][1] = ((Number) constraintData.getFieldValue("ymax")).floatValue();
            limits[2][0] = ((Number) constraintData.getFieldValue("zmin")).floatValue();
            limits[2][1] = ((Number) constraintData.getFieldValue("zmax")).floatValue();
        }
    }

    @Override
    public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
        if (this.getOwner() instanceof Bone && ((Bone) this.getOwner()).getParent() != null) {
            // location limit does not work on bones who have parent
            return;
        }

        Vector3f translation = ownerTransform.getTranslation();

        if ((flag & LIMIT_XMIN) != 0 && translation.x < limits[0][0]) {
            translation.x -= (translation.x - limits[0][0]) * influence;
        }
        if ((flag & LIMIT_XMAX) != 0 && translation.x > limits[0][1]) {
            translation.x -= (translation.x - limits[0][1]) * influence;
        }
        if ((flag & LIMIT_YMIN) != 0 && translation.y < limits[1][0]) {
            translation.y -= (translation.y - limits[1][0]) * influence;
        }
        if ((flag & LIMIT_YMAX) != 0 && translation.y > limits[1][1]) {
            translation.y -= (translation.y - limits[1][1]) * influence;
        }
        if ((flag & LIMIT_ZMIN) != 0 && translation.z < limits[2][0]) {
            translation.z -= (translation.z - limits[2][0]) * influence;
        }
        if ((flag & LIMIT_ZMAX) != 0 && translation.z > limits[2][1]) {
            translation.z -= (translation.z - limits[2][1]) * influence;
        }
    }

    @Override
    public String getConstraintTypeName() {
        return "Limit location";
    }
}
