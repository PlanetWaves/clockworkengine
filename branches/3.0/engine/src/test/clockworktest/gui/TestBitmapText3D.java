

package clockworktest.gui;

import com.clockwork.app.SimpleApplication;
import com.clockwork.font.BitmapFont;
import com.clockwork.font.BitmapText;
import com.clockwork.font.Rectangle;
import com.clockwork.renderer.queue.RenderQueue.Bucket;
import com.clockwork.scene.Geometry;
import com.clockwork.scene.shape.Quad;

public class TestBitmapText3D extends SimpleApplication {

    private String txtB =
    "ABCDEFGHIKLMNOPQRSTUVWXYZ1234567890`~!@#$%^&*()-=_+[]\\;',./{}|:<>?";

    public static void main(String[] args){
        TestBitmapText3D app = new TestBitmapText3D();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        Quad q = new Quad(6, 3);
        Geometry g = new Geometry("quad", q);
        g.setLocalTranslation(0, -3, -0.0001f);
        g.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        rootNode.attachChild(g);

        BitmapFont fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText txt = new BitmapText(fnt, false);
        txt.setBox(new Rectangle(0, 0, 6, 3));
        txt.setQueueBucket(Bucket.Transparent);
        txt.setSize( 0.5f );
        txt.setText(txtB);
        rootNode.attachChild(txt);
    }

}
