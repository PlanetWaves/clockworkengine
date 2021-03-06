

package clockworktest.gui;

import com.clockwork.app.SimpleApplication;
import com.clockwork.ui.Picture;

public class TestOrtho extends SimpleApplication {

    public static void main(String[] args){
        TestOrtho app = new TestOrtho();
        app.start();
    }

    public void simpleInitApp() {
        Picture p = new Picture("Picture");
        p.move(0, 0, -1); // make it appear behind stats view
        p.setPosition(0, 0);
        p.setWidth(settings.getWidth());
        p.setHeight(settings.getHeight());
        p.setImage(assetManager, "Interface/Logo/Monkey.png", false);

        // attach geometry to orthoNode
        guiNode.attachChild(p);
    }

}
