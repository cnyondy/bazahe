package bazahe.ui;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Liu Dong
 */
public class T {
    public static void main(String[] args) {
        boolean exists = Files.exists(Paths.get("test.p12"));
        System.out.println(exists);
    }
}
