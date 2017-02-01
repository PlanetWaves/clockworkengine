package com.clockwork.scene.plugins.blender.modifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.clockwork.animation.AnimControl;
import com.clockwork.animation.Animation;
import com.clockwork.animation.SpatialTrack;
import com.clockwork.scene.Node;
import com.clockwork.scene.Spatial;
import com.clockwork.scene.plugins.blender.BlenderContext;
import com.clockwork.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.clockwork.scene.plugins.blender.animations.AnimationData;
import com.clockwork.scene.plugins.blender.animations.Ipo;
import com.clockwork.scene.plugins.blender.exceptions.BlenderFileException;

/**
 * This modifier allows to add animation to the object.
 * 
 */
/* package */class ObjectAnimationModifier extends Modifier {
    private static final Logger LOGGER = Logger.getLogger(ObjectAnimationModifier.class.getName());

    /** Loaded animation data. */
    private AnimationData            animationData;

    /**
     * This constructor reads animation of the object itself (without bones) and
     * stores it as an ArmatureModifierData modifier. The animation is returned
     * as a modifier. It should be later applied regardless other modifiers. The
     * reason for this is that object may not have modifiers added but it's
     * animation should be working. The stored modifier is an anim data and
     * additional data is given object's OMA.
     * 
     * @param ipo
     *            the object's interpolation curves
     * @param objectAnimationName
     *            the name of object's animation
     * @param objectOMA
     *            the OMA of the object
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             this exception is thrown when the blender file is somehow
     *             corrupted
     */
    public ObjectAnimationModifier(Ipo ipo, String objectAnimationName, Long objectOMA, BlenderContext blenderContext) throws BlenderFileException {
        int fps = blenderContext.getBlenderKey().getFps();

        Spatial object = (Spatial) blenderContext.getLoadedFeature(objectOMA, LoadedFeatureDataType.LOADED_FEATURE);
        // calculating track
        SpatialTrack track = (SpatialTrack) ipo.calculateTrack(-1, object.getLocalRotation(), 0, ipo.getLastFrame(), fps, true);

        Animation animation = new Animation(objectAnimationName, ipo.getLastFrame() / (float) fps);
        animation.setTracks(new SpatialTrack[] { track });
        ArrayList<Animation> animations = new ArrayList<Animation>(1);
        animations.add(animation);

        animationData = new AnimationData(animations);
        blenderContext.setAnimData(objectOMA, animationData);
    }

    @Override
    public Node apply(Node node, BlenderContext blenderContext) {
        if (invalid) {
            LOGGER.log(Level.WARNING, "Armature modifier is invalid! Cannot be applied to: {0}", node.getName());
        }// if invalid, animData will be null
        if (animationData != null) {
            // INFO: constraints for this modifier are applied in the
            // ObjectHelper when the whole object is loaded
            List<Animation> animList = animationData.anims;
            if (animList != null && animList.size() > 0) {
                HashMap<String, Animation> anims = new HashMap<String, Animation>();
                for (int i = 0; i < animList.size(); ++i) {
                    Animation animation = animList.get(i);
                    anims.put(animation.getName(), animation);
                }

                AnimControl control = new AnimControl(null);
                control.setAnimations(anims);
                node.addControl(control);
            }
        }
        return node;
    }
}