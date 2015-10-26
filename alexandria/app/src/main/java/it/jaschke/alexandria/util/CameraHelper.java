package it.jaschke.alexandria.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import java.math.BigDecimal;
import java.util.List;

public class CameraHelper {

    private final static String TAG = CameraHelper.class.getSimpleName();
    private static final int TARGET_RESOLUTION = 640*480;

    public static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            for(int id = 0; id < Camera.getNumberOfCameras(); id++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(id, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    c = Camera.open(id); // attempt to get a Camera instance
                    if (c != null) {
                        c.enableShutterSound(false);
                    }
                    break;
                }
            }
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    /**
     * Finds the best supported resolution that is closest to a ratio passed in. For example, pass in
     * camera, 4, 3 for 4:3 ratio. Assumes the "best" is something like 640x480. A super high
     * resolution might cause memory problems when we are grabbing raw shots.
     */
    public static Camera.Size findBestSize(Camera camera, int ratioWidth, int ratioHeight) {
        Camera.Size bestSize = null;

        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        int bestRatioDelta = Integer.MAX_VALUE;
        int bestResolutionDelta = Integer.MAX_VALUE;

        int targetRatio = findRatio(ratioWidth, ratioHeight).intValue();
        Log.d(TAG, "Target ratio is " + targetRatio);
        for(Camera.Size size : sizes) {
            Log.d(TAG, "SUPPORTED SIZE: " + size.width + " " + size.height);
            int ratioDelta = Math.abs(targetRatio - findRatio(size.width, size.height).intValue());
            Log.d(TAG, "Ratio delta is " + ratioDelta);

            // We already have a closer ratio, keep looking...
            if (ratioDelta > bestRatioDelta)
                continue;

            // We already have a better tie breaker, keep looking...
            int resolutionDelta = Math.abs(TARGET_RESOLUTION - size.width * size.height);
            Log.d(TAG, "Resolution delta is " + resolutionDelta);
            if (bestRatioDelta == 0 && resolutionDelta > bestResolutionDelta)
                continue;

            Log.d(TAG, "Best size so far: " + size.width + " " + size.height);
            bestSize = size;
            bestRatioDelta = ratioDelta;
            bestResolutionDelta = resolutionDelta;
        }

        Log.d(TAG, "Best size: " + bestSize.width + " " + bestSize.height);
        return bestSize;
    }

    private static BigDecimal findRatio(int width, int height) {
        BigDecimal bdWidth = BigDecimal.valueOf(width);
        BigDecimal bdHeight = BigDecimal.valueOf(height);
        return bdWidth
                .divide(bdHeight, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(1000L));
    }


}
