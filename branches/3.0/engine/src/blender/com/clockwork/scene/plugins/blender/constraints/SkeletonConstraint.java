package com.clockwork.scene.plugins.blender.constraints;

import java.util.logging.Logger;

import com.clockwork.scene.plugins.blender.BlenderContext;
import com.clockwork.scene.plugins.blender.animations.Ipo;
import com.clockwork.scene.plugins.blender.exceptions.BlenderFileException;
import com.clockwork.scene.plugins.blender.file.Structure;

/**
 * Constraint applied on the skeleton. This constraint is here only to make the
 * application not crash when loads constraints applied to armature. But
 * skeleton movement is not supported by CW so the constraint will never be
 * applied.
 * 
 * 
 */
/* package */class SkeletonConstraint extends Constraint {
    private static final Logger LOGGER = Logger.getLogger(SkeletonConstraint.class.getName());

    public SkeletonConstraint(Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
        super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
    }

    @Override
    public boolean validate() {
        LOGGER.warning("Constraints for skeleton are not supported.");
        return false;
    }

    @Override
    public void apply(int frame) {
        LOGGER.warning("Applying constraints to skeleton is not supported.");
    }
}
