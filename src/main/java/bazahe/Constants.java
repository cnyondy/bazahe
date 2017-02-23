package bazahe;

import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Liu Dong
 */
public class Constants {

    public static final int rootCertificateValidates = 3650;

    private static final Path parentPath = Paths.get(System.getProperty("user.home"), ".bazahe");

    @SneakyThrows
    public static Path getParentPath() {
        if (!Files.exists(parentPath)) {
            Files.createDirectory(parentPath);
        }
        return parentPath;
    }
}
