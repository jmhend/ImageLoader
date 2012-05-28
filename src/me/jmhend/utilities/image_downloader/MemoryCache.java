package me.jmhend.utilities.image_downloader;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import android.graphics.Bitmap;
import android.util.Log;

public class MemoryCache {

    private static final String TAG = "MemoryCache";
    private Map<String, Bitmap> cache = Collections.synchronizedMap(new LinkedHashMap<String, Bitmap>(10,1.5f,true));
    private long sizeAllocated= 0; 
    private long maxAllocationLimit = 1000000; 

    public MemoryCache() {
        // Limits 'cache' to 25% of the Heap size.
        maxAllocationLimit = Runtime.getRuntime().maxMemory() / 4 ;
    }
    
    public void setLimit(long limit) {
        maxAllocationLimit = limit;
        Log.i(TAG, "MemoryCache will use up to " + maxAllocationLimit/(1024.0*1024.0) + "MB");
    }

    public Bitmap getBitmap(String id) {
        if(!cache.containsKey(id)) {
        	Log.i(TAG, "MemoryCache doesn't contain key: " + id);
        	return null;
        }   
        return cache.get(id);
    }

    public void putBitmap(String id, Bitmap bitmap) {
        try {
            if (cache.containsKey(id)) sizeAllocated -= getSizeInBytes(cache.get(id));
            cache.put(id, bitmap);
            sizeAllocated += getSizeInBytes(bitmap);
            checkSize();
        } catch(Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void checkSize() {
        Log.i(TAG, "Cache size = "+sizeAllocated+"; length = " + cache.size());
        if(sizeAllocated > maxAllocationLimit){
            Iterator<Entry<String, Bitmap>> iter = cache.entrySet().iterator(); //least recently accessed item will be the first one iterated  
            while(iter.hasNext()) {
                Entry<String, Bitmap> entry = iter.next();
                sizeAllocated -= getSizeInBytes(entry.getValue());
                iter.remove();
                if (sizeAllocated <= maxAllocationLimit) break;
            }
            Log.i(TAG, "Clean cache. New size: " + cache.size());
        }
    }

    public void clear() {
        cache.clear();
        sizeAllocated = 0;
    }

    long getSizeInBytes(Bitmap bitmap) {
        if(bitmap == null) return 0;
        return bitmap.getRowBytes() * bitmap.getHeight();
    }
}