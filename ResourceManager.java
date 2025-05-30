import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager {
    private final String resourceDir;
    private final Map<String, String> resources; // nome -> path

    public ResourceManager(String resourceDir) {
        this.resourceDir = resourceDir;
        this.resources = new ConcurrentHashMap<>();
        createResourceDir();
    }

    private void createResourceDir() {
        File dir = new File(resourceDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public boolean addResource(String name, String content) {
        try {
            File file = new File(resourceDir, name);
            Files.write(file.toPath(), content.getBytes());
            resources.put(name, file.getAbsolutePath());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public String getResource(String name) {
        String path = resources.get(name);
        if (path == null) return null;
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            return null;
        }
    }

    public Set<String> listResources() {
        return new HashSet<>(resources.keySet());
    }
}