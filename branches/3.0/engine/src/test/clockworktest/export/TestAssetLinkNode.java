

package clockworktest.export;

import com.clockwork.app.SimpleApplication;
import com.clockwork.asset.AssetKey;
import com.clockwork.asset.ModelKey;
import com.clockwork.export.binary.BinaryExporter;
import com.clockwork.export.binary.BinaryImporter;
import com.clockwork.light.DirectionalLight;
import com.clockwork.light.PointLight;
import com.clockwork.material.Material;
import com.clockwork.math.ColorRGBA;
import com.clockwork.math.FastMath;
import com.clockwork.math.Vector3f;
import com.clockwork.scene.AssetLinkNode;
import com.clockwork.scene.Geometry;
import com.clockwork.scene.Node;
import com.clockwork.scene.Spatial;
import com.clockwork.scene.shape.Sphere;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestAssetLinkNode extends SimpleApplication {

    float angle;
    PointLight pl;
    Spatial lightMdl;

    public static void main(String[] args){
        TestAssetLinkNode app = new TestAssetLinkNode();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        AssetLinkNode loaderNode=new AssetLinkNode();
        loaderNode.addLinkedChild(new ModelKey("Models/MonkeyHead/MonkeyHead.mesh.xml"));
        //load/attach the children (happens automatically on load)
//        loaderNode.attachLinkedChildren(assetManager);
//        rootNode.attachChild(loaderNode);

        //save and load the loaderNode
        try {
            //export to byte array
            ByteArrayOutputStream bout=new ByteArrayOutputStream();
            BinaryExporter.getInstance().save(loaderNode, bout);
            //import from byte array, automatically loads the monkeyhead from file
            ByteArrayInputStream bin=new ByteArrayInputStream(bout.toByteArray());
            BinaryImporter imp=BinaryImporter.getInstance();
            imp.setAssetManager(assetManager);
            Node newLoaderNode=(Node)imp.load(bin);
            //attach to rootNode
            rootNode.attachChild(newLoaderNode);
        } catch (IOException ex) {
            Logger.getLogger(TestAssetLinkNode.class.getName()).log(Level.SEVERE, null, ex);
        }


        rootNode.attachChild(loaderNode);

        lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
        lightMdl.setMaterial( (Material) assetManager.loadAsset(new AssetKey("Common/Materials/RedColor.j3m")));
        rootNode.attachChild(lightMdl);

        // flourescent main light
        pl = new PointLight();
        pl.setColor(new ColorRGBA(0.88f, 0.92f, 0.95f, 1.0f));
        rootNode.addLight(pl);

        // sunset light
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f,-0.7f,1).normalizeLocal());
        dl.setColor(new ColorRGBA(0.44f, 0.30f, 0.20f, 1.0f));
        rootNode.addLight(dl);

        // skylight
        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.6f,-1,-0.6f).normalizeLocal());
        dl.setColor(new ColorRGBA(0.10f, 0.22f, 0.44f, 1.0f));
        rootNode.addLight(dl);

        // white ambient light
        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(1, -0.5f,-0.1f).normalizeLocal());
        dl.setColor(new ColorRGBA(0.50f, 0.40f, 0.50f, 1.0f));
        rootNode.addLight(dl);
    }

    @Override
    public void simpleUpdate(float tpf){
        angle += tpf * 0.25f;
        angle %= FastMath.TWO_PI;

        pl.setPosition(new Vector3f(FastMath.cos(angle) * 6f, 3f, FastMath.sin(angle) * 6f));
        lightMdl.setLocalTranslation(pl.getPosition());
    }

}
