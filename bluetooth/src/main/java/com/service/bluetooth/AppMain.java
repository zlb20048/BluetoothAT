package com.service.bluetooth;

import android.app.Application;
import android.provider.Settings;
import com.service.bluetooth.BluetoothManager.ConnectListener;

/**
 * Created by liuzixiang on 16-3-30.
 */
public class AppMain extends Application
{
    private final static String LOG_TAG = "bt_service";

    @Override
    public void onCreate()
    {
        super.onCreate();
        WLog.initialize(LOG_TAG, WLog.LEVEL_VERBOSE);
        WLog.d("onCreate");
        final BluetoothManager manager = BluetoothManager.getDefault(getApplicationContext());
        manager.registerListener(new ConnectListener()
        {
            @Override
            public void onServiceConnected()
            {
                if (isCustomBtSwitchOn())
                {
                    manager.enable();
                }
                else
                {
                    manager.disable(true);
                }
            }

            @Override
            public void onServiceDisConnected()
            {

            }
        });
    }

    private boolean isCustomBtSwitchOn()
    {
        return (Settings.Global.getInt(getApplicationContext().getContentResolver(), Settings.Global.BLUETOOTH_ON, 0) == 1);
    }
}
