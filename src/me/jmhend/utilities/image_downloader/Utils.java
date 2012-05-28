package me.jmhend.utilities.image_downloader;

import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    public static void CopyStream(InputStream inputStream, OutputStream outputStream) {
        final int bufferSize = 1024;
        try {
            byte[] bytes = new byte[bufferSize];
            while (true) {
              int count = inputStream.read(bytes, 0, bufferSize);
              if (count == -1) break;
              outputStream.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }
}