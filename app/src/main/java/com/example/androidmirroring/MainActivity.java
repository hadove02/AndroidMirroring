package com.example.androidmirroring;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MediaProjectionDemo";
    private static final int PERMISSION_CODE = 1;
    /*
    private static final List<Resolution> RESOLUTIONS = Arrays.asList(
            new Resolution(640, 360),
            new Resolution(960, 540),
            new Resolution(1366, 768),
            new Resolution(1600, 900));

     */
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private int mDisplayWidth = 1200;
    private int mDisplayHeight = 1600;
    private boolean mScreenSharing;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private Surface mSurface;
    private SurfaceView mSurfaceView;
    private ToggleButton mToggle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //dpi 설정
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;


        mSurfaceView = findViewById(R.id.surface);
        mSurface = mSurfaceView.getHolder().getSurface();

        //권한 얻기
        mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mToggle = (ToggleButton) findViewById(R.id.toggle);
        mToggle.setSaveEnabled(false);
    }

    @Override
    protected void onStop() {
        stopScreenSharing();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "User denied screen sharing permission", Toast.LENGTH_SHORT).show();
            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(new MediaProjectionCallback(), null);
        mVirtualDisplay = createVirtualDisplay();
    }

    public void onToggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {

            Intent serviceIntent = new Intent(this, ForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            shareScreen();
        } else {
            stopScreenSharing();
        }
    }

    private void shareScreen() {
        mScreenSharing = true;
        if (mSurface == null) {
            return;
        }
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(),
                    PERMISSION_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
    }

    private void stopScreenSharing() {
        if (mToggle.isChecked()) {
            mToggle.setChecked(false);
        }
        mScreenSharing = false;
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("ScreenSharingDemo",
                mDisplayWidth, mDisplayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null /*Callbacks*/, null /*Handler*/);
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mMediaProjection = null;
            stopScreenSharing();
        }
    }
}