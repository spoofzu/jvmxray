package org.jvmxray.agent.util;

import java.io.*;

public class FileUtil {

    private static FileUtil fu = null;

    private FileUtil() {}

    public static synchronized final FileUtil getInstance() {
        if( fu == null ) {
            fu = new FileUtil();
        }
        return fu;
    }

    public void cp(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
}
