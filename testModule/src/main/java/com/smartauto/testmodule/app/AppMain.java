package com.smartauto.testmodule.app;

import android.app.Application;
import com.service.bluetooth.BluetoothManager;
import com.service.bluetooth.WLog;

/**
 * Created by liuzixiang on 16-4-14.
 */
public class AppMain extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        WLog.d("AppMain onCreate...");
        BluetoothManager manager = BluetoothManager.getDefault(getApplicationContext());
    }
}
