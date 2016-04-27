package com.service.bluetooth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by liuzixiang on 16-3-30.
 */
public class ATService extends Service
{
    private final static String TAG = ATService.class.getSimpleName();

    BtManagerService btManagerService = null;

    @Override
    public void onCreate()
    {
        super.onCreate();
        WLog.d(TAG, "onCreate...");
        btManagerService = new BtManagerService(getApplicationContext());
        btManagerService.setCommandInterface(new BluetoothInterfaceLayer(getApplicationContext()));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        WLog.d(TAG, "onStartCommand...");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        WLog.d(TAG, "onDestroy...");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        WLog.d(TAG, "onBind...");
        return btManagerService;
    }
}
