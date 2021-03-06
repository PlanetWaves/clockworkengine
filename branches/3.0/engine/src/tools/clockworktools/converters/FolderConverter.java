

package clockworktools.converters;

import com.clockwork.asset.AssetManager;
import com.clockwork.system.CWSystem;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class FolderConverter {

    private static AssetManager assetManager;
    private static File sourceRoot;
    private static JarOutputStream jarOut;
    private static long time;

    private static void process(File file) throws IOException{
        String name = file.getName().replaceAll("[\\/\\.]", "_");
        JarEntry entry = new JarEntry(name);
        entry.setTime(time);

        jarOut.putNextEntry(entry);
    }

    public static void main(String[] args) throws IOException{
        if (args.length == 0){
            System.out.println("Usage: java -jar FolderConverter <input folder>");
            System.out.println();
            System.out.println("  Converts files from input to output");
            System.exit(1);
        }

        sourceRoot = new File(args[0]);
        
        File jarFile = new File(sourceRoot.getParent(), sourceRoot.getName()+".jar");
        FileOutputStream out = new FileOutputStream(jarFile);
        jarOut = new JarOutputStream(out);

        assetManager = CWSystem.newAssetManager();
        assetManager.registerLocator(sourceRoot.toString(), 
                                     "com.clockwork.asset.plugins.FileSystemLocator");
        for (File f : sourceRoot.listFiles()){
             process(f);
        }
    }

}
