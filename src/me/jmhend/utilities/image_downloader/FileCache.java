package me.jmhend.utilities.image_downloader;

import java.io.File;
import java.net.URLEncoder;

import android.content.Context;

public class FileCache {
    
    private File cacheDir;
    
    public FileCache(Context context, String dirName) {
        // Find the directory to save cached images.
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), dirName);
        else
            cacheDir = context.getCacheDir();
        if(!cacheDir.exists())
            cacheDir.mkdirs();
    }
    
    public File getFile(String url){
        String filename = URLEncoder.encode(url);
        File file = new File(cacheDir, filename);
        return file;
    }
    
    public void clear() {
        File[] files = cacheDir.listFiles();
        if (files == null) return;
        for(File file : files) {
            file.delete();
        }
    }
}