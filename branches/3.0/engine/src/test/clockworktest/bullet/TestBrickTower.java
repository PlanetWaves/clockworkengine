
package clockworktest.bullet;



import com.clockwork.app.SimpleApplication;
import com.clockwork.asset.TextureKey;
import com.clockwork.bullet.BulletAppState;
import com.clockwork.bullet.PhysicsSpace;
import com.clockwork.bullet.collision.shapes.SphereCollisionShape;
import com.clockwork.bullet.control.RigidBodyControl;
import com.clockwork.font.BitmapText;
import com.clockwork.input.MouseInput;
import com.clockwork.input.controls.ActionListener;
import com.clockwork.input.controls.MouseButtonTrigger;
import com.clockwork.material.Material;
import com.clockwork.math.Vector2f;
import com.clockwork.math.Vector3f;
import com.clockwork.renderer.queue.RenderQueue.ShadowMode;
import com.clockwork.scene.Geometry;
import com.clockwork.scene.shape.Box;
import com.clockwork.scene.shape.Sphere;
import com.clockwork.scene.shape.Sphere.TextureMode;
import com.clockwork.shadow.PssmShadowRenderer;
import com.clockwork.shadow.PssmShadowRenderer.CompareMode;
import com.clockwork.shadow.PssmShadowRenderer.FilterMode;
import com.clockwork.texture.Texture;
import com.clockwork.texture.Texture.WrapMode;

/**
 *
 */
public class TestBrickTower extends SimpleApplication {

    int bricksPerLayer = 8;
    int brickLayers = 30;

    static float brickWidth = .75f, brickHeight = .25f, brickDepth = .25f;
    float radius = 3f;
    float angle = 0;


    Material mat;
    Material mat2;
    Material mat3;
    PssmShadowRenderer bsr;
    private Sphere bullet;
    private Box brick;
    private SphereCollisionShape bulletCollisionShape;

    private BulletAppState bulletAppState;

    public static void main(String args[]) {
        TestBrickTower f = new TestBrickTower();
        f.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
     //   bulletAppState.setEnabled(false);
        stateManager.attach(bulletAppState);
        bullet = new Sphere(32, 32, 0.4f, true, false);
        bullet.setTextureMode(TextureMode.Projected);
        bulletCollisionShape = new SphereCollisionShape(0.4f);

        brick = new Box(Vector3f.ZERO, brickWidth, brickHeight, brickDepth);
        brick.scaleTextureCoordinates(new Vector2f(1f, .5f));
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        initMaterial();
        initTower();
        initFloor();
        initCrossHairs();
        this.cam.setLocation(new Vector3f(0, 25f, 8f));
        cam.lookAt(Vector3f.ZERO, new Vector3f(0, 1, 0));
        cam.setFrustumFar(80);
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");
        rootNode.setShadowMode(ShadowMode.Off);
        bsr = new PssmShadowRenderer(assetManager, 1024, 2);
        bsr.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        bsr.setLambda(0.55f);
        bsr.setShadowIntensity(0.6f);
        bsr.setCompareMode(CompareMode.Hardware);
        bsr.setFilterMode(FilterMode.PCF4);
        viewPort.addProcessor(bsr);
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
    private ActionListener actionListener = new ActionListener() {

        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("shoot") && !keyPressed) {
                Geometry bulletg = new Geometry("bullet", bullet);
                bulletg.setMaterial(mat2);
                bulletg.setShadowMode(ShadowMode.CastAndReceive);
                bulletg.setLocalTranslation(cam.getLocation());
                RigidBodyControl bulletNode = new BombControl(assetManager, bulletCollisionShape, 1);
//                RigidBodyControl bulletNode = new RigidBodyControl(bulletCollisionShape, 1);
                bulletNode.setLinearVelocity(cam.getDirection().mult(25));
                bulletg.addControl(bulletNode);
                rootNode.attachChild(bulletg);
                getPhysicsSpace().add(bulletNode);
            }
        }
    };

    public void initTower() {
        double tempX = 0;
        double tempY = 0;
        double tempZ = 0;
        angle = 0f;
        for (int i = 0; i < brickLayers; i++){
            // Increment rows
            if(i!=0)
                tempY+=brickHeight*2;
            else
                tempY=brickHeight;
            // Alternate brick seams
            angle = 360.0f / bricksPerLayer * i/2f;
            for (int j = 0; j < bricksPerLayer; j++){
              tempZ = Math.cos(Math.toRadians(angle))*radius;
              tempX = Math.sin(Math.toRadians(angle))*radius;
              System.out.println("x="+((float)(tempX))+" y="+((float)(tempY))+" z="+(float)(tempZ));
              Vector3f vt = new Vector3f((float)(tempX), (float)(tempY), (float)(tempZ));
              // Add crenelation
              if (i==brickLayers-1){
                if (j%2 == 0){
                    addBrick(vt);
                }
              }
              // Create main tower
              else {
                addBrick(vt);
              }
              angle += 360.0/bricksPerLayer;
            }
          }

    }

    public void initFloor() {
        Box floorBox = new Box(Vector3f.ZERO, 10f, 0.1f, 5f);
        floorBox.scaleTextureCoordinates(new Vector2f(3, 6));

        Geometry floor = new Geometry("floor", floorBox);
        floor.setMaterial(mat3);
        floor.setShadowMode(ShadowMode.Receive);
        floor.setLocalTranslation(0, 0, 0);
        floor.addControl(new RigidBodyControl(0));
        this.rootNode.attachChild(floor);
        this.getPhysicsSpace().add(floor);
    }

    public void initMaterial() {
        mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
        key.setGenerateMips(true);
        Texture tex = assetManager.loadTexture(key);
        mat.setTexture("ColorMap", tex);

        mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
        key2.setGenerateMips(true);
        Texture tex2 = assetManager.loadTexture(key2);
        mat2.setTexture("ColorMap", tex2);

        mat3 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key3 = new TextureKey("Textures/Terrain/Pond/Pond.jpg");
        key3.setGenerateMips(true);
        Texture tex3 = assetManager.loadTexture(key3);
        tex3.setWrap(WrapMode.Repeat);
        mat3.setTexture("ColorMap", tex3);
    }

    public void addBrick(Vector3f ori) {
        Geometry reBoxg = new Geometry("brick", brick);
        reBoxg.setMaterial(mat);
        reBoxg.setLocalTranslation(ori);
        reBoxg.rotate(0f, (float)Math.toRadians(angle) , 0f );
        reBoxg.addControl(new RigidBodyControl(1.5f));
        reBoxg.setShadowMode(ShadowMode.CastAndReceive);
        reBoxg.getControl(RigidBodyControl.class).setFriction(1.6f);
        this.rootNode.attachChild(reBoxg);
        this.getPhysicsSpace().add(reBoxg);
    }

    protected void initCrossHairs() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }
}