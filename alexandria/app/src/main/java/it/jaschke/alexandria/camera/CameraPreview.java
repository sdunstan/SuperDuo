package it.jaschke.alexandria.camera;

/*
 * Barebones implementation of displaying camera preview.
 *
 * Created by lisah0 on 2012-02-24
 */

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.oned.MultiFormatOneDReader;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import it.jaschke.alexandria.MainActivity;
import it.jaschke.alexandria.util.CameraHelper;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback {
    public static final String BROADCAST_ACTION = "it.jaschkle.alexandria.BROADCAST_ACTION";
    public static final String EXTRA_PARSE_RESULT = "it.jaschkle.alexandria.PARSE_RESULT";
    private static final String TAG = CameraPreview.class.getName();

    private boolean isPortrait = true;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public CameraPreview(Context context) {
        super(context);
        init(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private boolean hasCamera() {
        return getCamera() != null;
    }

    private Camera getCamera() {
        MainActivity mainActivity = (MainActivity) getContext();
        return mainActivity.getCamera();
    }

    public void init(Context context) {
        Log.d(TAG, "Init CameraPreview looking for bar codes.");
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        getHolder().addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (!CameraHelper.checkCameraHardware(context)) {
            Log.w(TAG, "Device does not have a camera?");
            return;
        }

        scheduleAutofocus();
    }

    private void scheduleAutofocus() {
        Log.d(TAG, "Scheduling autofocus.");
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                autoFocus();
            }
        }, 3, TimeUnit.SECONDS);
    }

    /**
     * Make the preview fill the width in 4:3 size.
     * @param width
     * @param height
     */
    @Override
    protected void onMeasure(int width, int height) {
        int scaledHeight = MeasureSpec.getSize(width) * 3/4;
        int specHeight = MeasureSpec.makeMeasureSpec(scaledHeight, MeasureSpec.EXACTLY);
        super.onMeasure(width, specHeight);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasCamera())
            return;
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            getCamera().setPreviewDisplay(holder);
            getCamera().startPreview();
        } catch (IOException e) {
            Log.d("DBG", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (!hasCamera())
            return;
        getCamera().stopPreview();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!hasCamera())
            return;
        /*
         * If your preview can change or rotate, take care of those events here.
         * Make sure to stop the preview before resizing or reformatting it.
         */
        if (holder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            getCamera().stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        try {
            // Hard code camera surface rotation 90 degs to match Activity view in portrait
            // getCamera().setDisplayOrientation(90);
            rotate();

            getCamera().setPreviewDisplay(holder);
            getCamera().startPreview();
        } catch (Exception e){
            Log.d("DBG", "Error starting camera preview: " + e.getMessage());
        }
    }

    private void rotate() {
        if (!hasCamera())
            return;

        int degrees = 0;

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        switch (windowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        degrees = (info.orientation - degrees + 360) % 360;
        Log.i(TAG, "Display orientation is being set to " + degrees);

        isPortrait = (degrees == 90 || degrees == 270);
        getCamera().setDisplayOrientation(degrees);
    }

    public void autoFocus() {
        if (!hasCamera())
            return;
        getCamera().autoFocus(this);
    }

    private void broadcastResult(String result) {
        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(EXTRA_PARSE_RESULT, result);
        LocalBroadcastManager.getInstance(this.getContext()).sendBroadcast(intent);
    }

    @Override
    public void onAutoFocus(boolean b, Camera camera) {
        Log.d(TAG, "Got autofocus");
        if (camera != null) {
            camera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bytes, Camera camera) {
                    Log.i(TAG, "Got preview frame..." + (bytes != null ? bytes.length : 0));
                    if (bytes != null && bytes.length > 0) {
                        Camera.Size size = camera.getParameters().getPreviewSize();
                        RawImage raw = new RawImage(bytes, size.width, size.height);
                        new ScanBarcodeTask().execute(raw);
                    }
                }
            });
        }
    }

    private class ScanBarcodeTask extends AsyncTask<RawImage, Integer, String> {

        @Override
        protected String doInBackground(RawImage... rawImages) {
            String stringResult = null;
            Result rawResult = null;
            RawImage raw = rawImages[0];

            if (isPortrait)
                raw = raw.rotate();

            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    raw.getData(), raw.getWidth(), raw.getHeight(), 0, 0,
                    raw.getWidth(), raw.getHeight(), false);

            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                hints.put(DecodeHintType.TRY_HARDER, Void.TYPE);
                MultiFormatOneDReader upcaReader = new MultiFormatOneDReader(hints);
                try {
                    rawResult = upcaReader.decode(bitmap, hints);
                    stringResult = rawResult.getText();
                } catch (NotFoundException e) {
                    Log.e(TAG, "no result found from barcode", e);
                } catch (FormatException e) {
                    Log.e(TAG, "barcode found but not in the correct format", e);
                } finally {
                    upcaReader.reset();
                }
            }

            return stringResult;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                scheduleAutofocus();
            }
            else {
                broadcastResult(result);
            }
        }
    }
}
