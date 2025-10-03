package core;

import java.io.File;

public class WebExecutor {

    public static void runWeb(File jar) throws Exception {
        // 直接启动 web jar
        new ProcessBuilder("java", "-jar", jar.getAbsolutePath()).start();
    }
}
