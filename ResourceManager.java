import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class ResourceManager {
    private final String resourceDir;
    private final Map<String, String> resources; // nome -> path
    private final Object resourceLock = new Object();
    private final String port;
    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());

    public ResourceManager(String resourceDir, String port) {
        this.resourceDir = resourceDir;
        this.resources = new ConcurrentHashMap<>();
        this.port = port;
        createResourceDir();
        createReceivedDir();
    }

    private void createResourceDir() {
        File dir = new File(resourceDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void createReceivedDir() {
        File dir = new File("received");
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public boolean addResource(String name, String content) {
        synchronized (resourceLock) {
            try {
                File file = new File(resourceDir, name);
                Files.write(file.toPath(), content.getBytes());
                resources.put(name, file.getAbsolutePath());
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public String getResource(String name) {
        synchronized (resourceLock) {
            String path = resources.get(name);
            if (path == null) return null;
            try {
                return new String(Files.readAllBytes(Paths.get(path)));
            } catch (IOException e) {
                return null;
            }
        }
    }

    public Set<String> listResources() {
        synchronized (resourceLock) {
            return new HashSet<>(resources.keySet());
        }
    }

    public void reload() {
        synchronized (resourceLock) {
            // Pulisci la mappa esistente
            resources.clear();
            
            // Leggi i file dalla cartella resources
            File resourcesDir = new File("resources");
            if (resourcesDir.exists() && resourcesDir.isDirectory()) {
                for (File peerDir : resourcesDir.listFiles()) {
                    if (peerDir.isDirectory()) {
                        for (File file : peerDir.listFiles()) {
                            if (file.isFile()) {
                                try {
                                    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                                    resources.put(file.getName(), content);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public String getResourcePath(String name) {
        synchronized (resourceLock) {
            return resources.get(name);
        }
    }
}