package me.jmhend.utilities.image_downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

public class ImageLoader {
	private static final String TAG = "ImageLoader";
	
    MemoryCache memoryCache = new MemoryCache();
    FileCache fileCache;
    private Map<ImageView, String> imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    ExecutorService executorService; 
    
    private String cacheDirectory = "PowerTrip";
    private int maxThreads = 5;
    private int loadingImageId = R.drawable.icon;
    private int downloadTimeout = 10000;
    private int imageSizeLimit = 120;
    private boolean limitImageSize = false;
    
    private Handler handlerToNotify = null;
    
    public ImageLoader(Context context){
        fileCache = new FileCache(context, cacheDirectory);
        executorService = Executors.newFixedThreadPool(maxThreads);
        limitImageSize = false;
    }
    
    public ImageLoader(Context context, Handler handlerToNotifyOnFinish){
        fileCache = new FileCache(context, cacheDirectory);
        executorService = Executors.newFixedThreadPool(maxThreads);
        limitImageSize = false;
        handlerToNotify = handlerToNotifyOnFinish;
    }
    
    public ImageLoader(Context context, int sizeLimit){
        fileCache = new FileCache(context, cacheDirectory);
        executorService = Executors.newFixedThreadPool(maxThreads);
        limitImageSize = true;
        imageSizeLimit = sizeLimit;
    }
    
    public void displayImage(String imageURL, ImageView destinationImageView) {
        imageViews.put(destinationImageView, imageURL);
        Bitmap bitmap = memoryCache.getBitmap(imageURL);
        
        if (bitmap != null) {
        	destinationImageView.setImageBitmap(bitmap);
        } else {
            queuePhoto(imageURL, destinationImageView);
            destinationImageView.setImageResource(loadingImageId);
        }
    }
        
    private void queuePhoto(String imageURL, ImageView destinationImageView) {
    	ImageToLoad imageToLoad = new ImageToLoad(imageURL, destinationImageView);
        executorService.submit(new ImageDownloader(imageToLoad));
    }
    
    private Bitmap getBitmap(String imageURL) 
    {
        File bitmapFile = fileCache.getFile(imageURL);
        
        // If this bitmap is in the fileCache, no need to download it, so return it.
        Bitmap bitmap = decodeFile(bitmapFile);
        if (bitmap != null) {
        	return bitmap;
        }
        
        // If not in the fileCache, download it to a file, then decode it:
        bitmap = null;
        try {
            URL URLOfImage = new URL(imageURL);
            HttpURLConnection connection = (HttpURLConnection) URLOfImage.openConnection();
            connection.setConnectTimeout(downloadTimeout);
            connection.setReadTimeout(downloadTimeout);
            connection.setInstanceFollowRedirects(true);
            
            InputStream inputStream = connection.getInputStream();
            OutputStream outputStream = new FileOutputStream(bitmapFile);
            Utils.CopyStream(inputStream, outputStream);
            outputStream.close();
            
            bitmap = decodeFile(bitmapFile);
            return bitmap;
            
        } catch (Exception ex) {
        	Log.e(TAG, "Unable to download and decode bitmap.");
        	ex.printStackTrace();
        	return null;
        }
    }

    // Decodes Bitmap from its file, and resizes according to inSampeSize
    private Bitmap decodeFile(File bitmapFile){
        try {
        	int scaleFactor = 1;
        	if (limitImageSize) {
	            // Only decode the Bitmap's dimensions first, not the actual Bitmap.
	            BitmapFactory.Options options = new BitmapFactory.Options();
	            options.inJustDecodeBounds = true;
	            BitmapFactory.decodeStream(new FileInputStream(bitmapFile), null, options);
	            
	            // Resize the Bitmap by powers of 2 until it's width and height are less than imageSizeLimit.
	            int imageWidth = options.outWidth; 
	            int imageHeight = options.outHeight;
	            
	            while (true){
	                if ((imageWidth / 2 < imageSizeLimit) || (imageHeight / 2 < imageSizeLimit)) {
	                	break;
	                }
	                imageWidth /= 2;
	                imageHeight /= 2;
	                scaleFactor *= 2;
	            }
        	}
            
            // Now decode the Bitmap with inSampleSize set as the above calculated scaleFactor.
            BitmapFactory.Options actualOptions = new BitmapFactory.Options();
            actualOptions.inSampleSize = scaleFactor;
            Bitmap resizedBitmap = BitmapFactory.decodeStream(new FileInputStream(bitmapFile), null, actualOptions);
            
            return resizedBitmap;
        } catch (FileNotFoundException e) {
        	e.printStackTrace();
        }
        return null;
    }
    
    private class ImageToLoad {
        public String imageURL;
        public ImageView destinationImageView;
        
        public ImageToLoad(String url, ImageView imageView){
        	imageURL = url; 
            destinationImageView=imageView;
        }
    }
    
    class ImageDownloader implements Runnable {
        ImageToLoad imageToLoad;
        
        ImageDownloader(ImageToLoad image) {
            imageToLoad = image;
        }
        
        @Override
        public void run() {
            if (imageViewReused(imageToLoad)) {
            	return;
            }
            
            Bitmap bitmap = getBitmap(imageToLoad.imageURL);
            memoryCache.putBitmap(imageToLoad.imageURL, bitmap);
            if (imageViewReused(imageToLoad)) {
            	return;
            }
            
            BitmapDisplayer bitmapDisplayer = new BitmapDisplayer(bitmap, imageToLoad);
            Activity sourceActivity = (Activity) imageToLoad.destinationImageView.getContext();
            sourceActivity.runOnUiThread(bitmapDisplayer);
        }
    }
    
    boolean imageViewReused(ImageToLoad imageToLoad){
        String tag = imageViews.get(imageToLoad.destinationImageView);
        if (tag == null || !tag.equals(imageToLoad.imageURL)) {
        	return true;
        }
        return false;
    }
    
    // Displays the Bitmap on the UI Thread.
    class BitmapDisplayer implements Runnable {
        Bitmap bitmap;
        ImageToLoad imageToLoad;
        
        public BitmapDisplayer(Bitmap bmp, ImageToLoad image) {
        	bitmap = bmp;
        	imageToLoad = image;
        }
        
        public void run() {
            if (imageViewReused(imageToLoad)) {
            	return;
            }
            if (bitmap != null) {
            	imageToLoad.destinationImageView.setImageBitmap(bitmap);
            	if (handlerToNotify != null)
            		handlerToNotify.sendEmptyMessage(0);
            } else
            	imageToLoad.destinationImageView.setImageBitmap(null);
        }
    }

    public void clearCache() {
        memoryCache.clear();
        fileCache.clear();
        Log.i(TAG, "Clearing file and memory caches.");
    }
    
    public void setCacheDirectory(String dir) {
    	cacheDirectory = dir;
    }
    
    public void setMaxThreads(int numThreads) {
    	maxThreads = numThreads;
    }
    
    public void setLoadingImageId(int imageResourceId) {
    	loadingImageId = imageResourceId;
    }
    
    public void setDownloadTimeout(int timeInMilliseconds) {
    	downloadTimeout = timeInMilliseconds;
    }
    
    public void setImageSizeLimit(int sizeInPixels) {
    	imageSizeLimit = sizeInPixels;
    }
    
    public void setLimitImageSize(boolean shouldLimitImageSize) {
    	limitImageSize = shouldLimitImageSize;
    }
}







