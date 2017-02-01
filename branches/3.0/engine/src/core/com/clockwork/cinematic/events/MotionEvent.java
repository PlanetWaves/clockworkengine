
package com.clockwork.cinematic.events;

import com.clockwork.animation.LoopMode;
import com.clockwork.app.Application;
import com.clockwork.cinematic.Cinematic;
import com.clockwork.cinematic.MotionPath;
import com.clockwork.cinematic.PlayState;
import com.clockwork.export.InputCapsule;
import com.clockwork.export.JmeExporter;
import com.clockwork.export.JmeImporter;
import com.clockwork.export.OutputCapsule;
import com.clockwork.math.Quaternion;
import com.clockwork.math.Vector3f;
import com.clockwork.renderer.RenderManager;
import com.clockwork.renderer.ViewPort;
import com.clockwork.scene.Spatial;
import com.clockwork.scene.control.Control;
import java.io.IOException;

/**
 * A MotionTrack is a control over the spatial that manage the position and direction of the spatial while following a motion Path
 *
 * You must first create a MotionPath and then create a MotionTrack to associate a spatial and the path.
 *
 */
public class MotionEvent extends AbstractCinematicEvent implements Control {

    protected Spatial spatial;
    protected int currentWayPoint;
    protected float currentValue;
    protected Vector3f direction = new Vector3f();
    protected Vector3f lookAt;
    protected Vector3f upVector = Vector3f.UNIT_Y;
    protected Quaternion rotation;
    protected Direction directionType = Direction.None;
    protected MotionPath path;
    private boolean isControl = true;
    /**
     * the distance traveled by the spatial on the path
     */
    protected float traveledDistance = 0;

    /**
     * Enum for the different type of target direction behavior
     */
    public enum Direction {

        /**
         * the target stay in the starting direction
         */
        None,
        /**
         * The target rotates with the direction of the path
         */
        Path,
        /**
         * The target rotates with the direction of the path but with the additon of a rtotation
         * you need to use the setRotation mathod when using this Direction
         */
        PathAndRotation,
        /**
         * The target rotates with the given rotation
         */
        Rotation,
        /**
         * The target looks at a point
         * You need to use the setLookAt method when using this direction
         */
        LookAt
    }

    /**
     * Create MotionTrack,
     * when using this constructor don't forget to assign spatial and path
     */
    public MotionEvent() {
        super();
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path
     * @param spatial
     * @param path
     */
    public MotionEvent(Spatial spatial, MotionPath path) {
        super();
        this.spatial = spatial;
        spatial.addControl(this);
        this.path = path;
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path
     * @param spatial
     * @param path
     */
    public MotionEvent(Spatial spatial, MotionPath path, float initialDuration) {
        super(initialDuration);
        this.spatial = spatial;
        spatial.addControl(this);
        this.path = path;
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path
     * @param spatial
     * @param path
     */
    public MotionEvent(Spatial spatial, MotionPath path, LoopMode loopMode) {
        super();
        this.spatial = spatial;
        spatial.addControl(this);
        this.path = path;
        this.loopMode = loopMode;
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path
     * @param spatial
     * @param path
     */
    public MotionEvent(Spatial spatial, MotionPath path, float initialDuration, LoopMode loopMode) {
        super(initialDuration);
        this.spatial = spatial;
        spatial.addControl(this);
        this.path = path;
        this.loopMode = loopMode;
    }

    public void update(float tpf) {
        if (isControl) {
            internalUpdate(tpf);
        }
    }

    @Override
    public void internalUpdate(float tpf) {
        if (playState == PlayState.Playing) {
            time = time + (tpf * speed);
            if (loopMode == loopMode.Loop && time < 0) {
                time = initialDuration;
            }
            if ((time >= initialDuration || time < 0) && loopMode == loopMode.DontLoop) {
                if (time >= initialDuration) {
                    path.triggerWayPointReach(path.getNbWayPoints() - 1, this);
                }
                stop();
            } else {
                onUpdate(tpf);
            }
        }
    }

    @Override
    public void initEvent(Application app, Cinematic cinematic) {
        super.initEvent(app, cinematic);
        isControl = false;
    }

    @Override
    public void setTime(float time) {
        super.setTime(time);
        onUpdate(0);
    }

    public void onUpdate(float tpf) {
        traveledDistance = path.interpolatePath(time, this, tpf);
        computeTargetDirection();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(lookAt, "lookAt", Vector3f.ZERO);
        oc.write(upVector, "upVector", Vector3f.UNIT_Y);
        oc.write(rotation, "rotation", Quaternion.IDENTITY);
        oc.write(directionType, "directionType", Direction.None);
        oc.write(path, "path", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        lookAt = (Vector3f) in.readSavable("lookAt", Vector3f.ZERO);
        upVector = (Vector3f) in.readSavable("upVector", Vector3f.UNIT_Y);
        rotation = (Quaternion) in.readSavable("rotation", Quaternion.IDENTITY);
        directionType = in.readEnum("directionType", Direction.class, Direction.None);
        path = (MotionPath) in.readSavable("path", null);
    }

    /**
     * this method is meant to be called by the motion path only
     * @return
     */
    public boolean needsDirection() {
        return directionType == Direction.Path || directionType == Direction.PathAndRotation;
    }

    private void computeTargetDirection() {
        switch (directionType) {
            case Path:
                Quaternion q = new Quaternion();
                q.lookAt(direction, upVector);
                spatial.setLocalRotation(q);
                break;
            case LookAt:
                if (lookAt != null) {
                    spatial.lookAt(lookAt, upVector);
                }
                break;
            case PathAndRotation:
                if (rotation != null) {
                    Quaternion q2 = new Quaternion();
                    q2.lookAt(direction, upVector);
                    q2.multLocal(rotation);
                    spatial.setLocalRotation(q2);
                }
                break;
            case Rotation:
                if (rotation != null) {
                    spatial.setLocalRotation(rotation);
                }
                break;
            case None:
                break;
            default:
                break;
        }
    }

    /**
     * Clone this control for the given spatial
     * @param spatial
     * @return
     */
    public Control cloneForSpatial(Spatial spatial) {
        MotionEvent control = new MotionEvent(spatial, path);
        control.playState = playState;
        control.currentWayPoint = currentWayPoint;
        control.currentValue = currentValue;
        control.direction = direction.clone();
        control.lookAt = lookAt.clone();
        control.upVector = upVector.clone();
        control.rotation = rotation.clone();
        control.initialDuration = initialDuration;
        control.speed = speed;
        control.loopMode = loopMode;
        control.directionType = directionType;

        return control;
    }

    @Override
    public void onPlay() {
        traveledDistance = 0;
    }

    @Override
    public void onStop() {
        currentWayPoint = 0;
    }

    @Override
    public void onPause() {
    }

    /**
     * this method is meant to be called by the motion path only
     * @return
     */
    public float getCurrentValue() {
        return currentValue;
    }

    /**
     * this method is meant to be called by the motion path only
     *
     */
    public void setCurrentValue(float currentValue) {
        this.currentValue = currentValue;
    }

    /**
     * this method is meant to be called by the motion path only
     * @return
     */
    public int getCurrentWayPoint() {
        return currentWayPoint;
    }

    /**
     * this method is meant to be called by the motion path only
     *
     */
    public void setCurrentWayPoint(int currentWayPoint) {
        this.currentWayPoint = currentWayPoint;
    }

    /**
     * returns the direction the spatial is moving
     * @return
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * Sets the direction of the spatial, using the Y axis as the up vector
     * Use MotionEvent#setDirection((Vector3f direction,Vector3f upVector) if 
     * you want a custum up vector.
     * This method is used by the motion path.
     * @param direction
     */
    public void setDirection(Vector3f direction) {
        setDirection(direction, Vector3f.UNIT_Y); 
   }
    
    /**
     * Sets the direction of the spatial witht ht egiven up vector
     * This method is used by the motion path.
     * @param direction
     * @param upVector the up vector to consider for this direction
     */
    public void setDirection(Vector3f direction,Vector3f upVector) {
        this.direction.set(direction);
        this.upVector.set(upVector);
    }

    /**
     * returns the direction type of the target
     * @return the direction type
     */
    public Direction getDirectionType() {
        return directionType;
    }

    /**
     * Sets the direction type of the target
     * On each update the direction given to the target can have different behavior
     * See the Direction Enum for explanations
     * @param directionType the direction type
     */
    public void setDirectionType(Direction directionType) {
        this.directionType = directionType;
    }

    /**
     * Set the lookAt for the target
     * This can be used only if direction Type is Direction.LookAt
     * @param lookAt the position to look at
     * @param upVector the up vector
     */
    public void setLookAt(Vector3f lookAt, Vector3f upVector) {
        this.lookAt = lookAt;
        this.upVector = upVector;
    }

    /**
     * returns the rotation of the target
     * @return the rotation quaternion
     */
    public Quaternion getRotation() {
        return rotation;
    }

    /**
     * sets the rotation of the target
     * This can be used only if direction Type is Direction.PathAndRotation or Direction.Rotation
     * With PathAndRotation the target will face the direction of the path multiplied by the given Quaternion.
     * With Rotation the rotation of the target will be set with the given Quaternion.
     * @param rotation the rotation quaternion
     */
    public void setRotation(Quaternion rotation) {
        this.rotation = rotation;
    }

    /**
     * retun the motion path this control follows
     * @return
     */
    public MotionPath getPath() {
        return path;
    }

    /**
     * Sets the motion path to follow
     * @param path
     */
    public void setPath(MotionPath path) {
        this.path = path;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            play();
        } else {
            pause();
        }
    }

    public boolean isEnabled() {
        return playState != PlayState.Stopped;
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
    }

    public Spatial getSpatial() {
        return spatial;
    }

    /**
     * return the distance traveled by the spatial on the path
     * @return 
     */
    public float getTraveledDistance() {
        return traveledDistance;
    }
}