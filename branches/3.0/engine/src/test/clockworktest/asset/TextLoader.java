package clockworktest.asset;

import com.clockwork.asset.AssetInfo;
import com.clockwork.asset.AssetLoader;
import java.io.IOException;
import java.util.Scanner;

/**
 * An example implementation of AssetLoader} to load text
 * files as strings.
 */
public class TextLoader implements AssetLoader {
    public Object load(AssetInfo assetInfo) throws IOException {
        Scanner scan = new Scanner(assetInfo.openStream());
        StringBuilder sb = new StringBuilder();
        try {
            while (scan.hasNextLine()) {
                sb.append(scan.nextLine()).append('\n');
            }
        } finally {
            scan.close();
        }
        return sb.toString();
    }
}
