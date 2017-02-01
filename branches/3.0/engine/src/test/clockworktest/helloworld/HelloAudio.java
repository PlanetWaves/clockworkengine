package clockworktest.helloworld;

import com.clockwork.app.SimpleApplication;
import com.clockwork.audio.AudioNode;
import com.clockwork.input.MouseInput;
import com.clockwork.input.controls.ActionListener;
import com.clockwork.input.controls.MouseButtonTrigger;
import com.clockwork.material.Material;
import com.clockwork.math.ColorRGBA;
import com.clockwork.scene.Geometry;
import com.clockwork.scene.shape.Box;

/** Sample 11 - playing 3D audio. */
public class HelloAudio extends SimpleApplication {

  private AudioNode audio_gun;
  private AudioNode audio_nature;
  private Geometry player;

  public static void main(String[] args) {
    HelloAudio app = new HelloAudio();
    app.start();
  }

  @Override
  public void simpleInitApp() {
    flyCam.setMoveSpeed(40);
    
    /** just a blue box floating in space */
    Box box1 = new Box(1, 1, 1);
    player = new Geometry("Player", box1);
    Material mat1 = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
    mat1.setColor("Color", ColorRGBA.Blue);
    player.setMaterial(mat1);
    rootNode.attachChild(player);

    /** custom init methods, see below */
    initKeys();
    initAudio();
  }

  /** We create two audio nodes. */
  private void initAudio() {
    /* gun shot sound is to be triggered by a mouse click. */
    audio_gun = new AudioNode(assetManager, "Sound/Effects/Gun.wav", false);
    audio_gun.setPositional(false);
    audio_gun.setLooping(false);
    audio_gun.setVolume(2);
    rootNode.attachChild(audio_gun);

    /* nature sound - keeps playing in a loop. */
    audio_nature = new AudioNode(assetManager, "Sound/Environment/Ocean Waves.ogg", true);
    audio_nature.setLooping(true);  // activate continuous playing
    audio_nature.setPositional(true);   
    audio_nature.setVolume(3);
    rootNode.attachChild(audio_nature);
    audio_nature.play(); // play continuously!
  }

  /** Declaring "Shoot" action, mapping it to a trigger (mouse left click). */
  private void initKeys() {
    inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(actionListener, "Shoot");
  }

  /** Defining the "Shoot" action: Play a gun sound. */
  private ActionListener actionListener = new ActionListener() {
    @Override
    public void onAction(String name, boolean keyPressed, float tpf) {
      if (name.equals("Shoot") && !keyPressed) {
        audio_gun.playInstance(); // play each instance once!
      }
    }
  };

  /** Move the listener with the a camera - for 3D audio. */
  @Override
  public void simpleUpdate(float tpf) {
    listener.setLocation(cam.getLocation());
    listener.setRotation(cam.getRotation());
  }

}