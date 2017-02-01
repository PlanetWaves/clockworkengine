

package clockworktest.light;

import com.clockwork.app.SimpleApplication;
import com.clockwork.material.Material;
import com.clockwork.math.ColorRGBA;
import com.clockwork.math.Quaternion;
import com.clockwork.math.Vector3f;
import com.clockwork.renderer.Camera;
import com.clockwork.renderer.queue.RenderQueue.ShadowMode;
import com.clockwork.scene.Geometry;
import com.clockwork.scene.Spatial;
import com.clockwork.scene.debug.WireFrustum;
import com.clockwork.scene.shape.Box;
import com.clockwork.shadow.BasicShadowRenderer;
import com.clockwork.shadow.ShadowUtil;

public class TestShadow extends SimpleApplication {

    float angle;
    Spatial lightMdl;
    Spatial teapot;
    Geometry frustumMdl;
    WireFrustum frustum;

    private BasicShadowRenderer bsr;
    private Vector3f[] points;

    {
        points = new Vector3f[8];
        for (int i = 0; i < points.length; i++) points[i] = new Vector3f();
    }

    public static void main(String[] args){
        TestShadow app = new TestShadow();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // put the camera in a bad position
        cam.setLocation(new Vector3f(0.7804813f, 1.7502685f, -2.1556435f));
        cam.setRotation(new Quaternion(0.1961598f, -0.7213164f, 0.2266092f, 0.6243975f));
        cam.setFrustumFar(10);

        Material mat = assetManager.loadMaterial("Common/Materials/WhiteColor.j3m");
        rootNode.setShadowMode(ShadowMode.Off);
        Box floor = new Box(Vector3f.ZERO, 3, 0.1f, 3);
        Geometry floorGeom = new Geometry("Floor", floor);
        floorGeom.setMaterial(mat);
        floorGeom.setLocalTranslation(0,-0.2f,0);
        floorGeom.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(floorGeom);

        teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        teapot.setLocalScale(2f);
        teapot.setMaterial(mat);
        teapot.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(teapot);
//        lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
//        lightMdl.setMaterial(mat);
//        // disable shadowing for light representation
//        lightMdl.setShadowMode(ShadowMode.Off);
//        rootNode.attachChild(lightMdl);

        bsr = new BasicShadowRenderer(assetManager, 512);
        bsr.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        viewPort.addProcessor(bsr);

        frustum = new WireFrustum(bsr.getPoints());
        frustumMdl = new Geometry("f", frustum);
        frustumMdl.setCullHint(Spatial.CullHint.Never);
        frustumMdl.setShadowMode(ShadowMode.Off);
        frustumMdl.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        frustumMdl.getMaterial().getAdditionalRenderState().setWireframe(true);
        frustumMdl.getMaterial().setColor("Color", ColorRGBA.Red);
        rootNode.attachChild(frustumMdl);
    }

    @Override
    public void simpleUpdate(float tpf){
        Camera shadowCam = bsr.getShadowCamera();
        ShadowUtil.updateFrustumPoints2(shadowCam, points);

        frustum.update(points);
        // rotate teapot around Y axis
        teapot.rotate(0, tpf * 0.25f, 0);
    }

}