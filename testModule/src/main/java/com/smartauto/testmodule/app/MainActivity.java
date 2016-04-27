package com.smartauto.testmodule.app;

import android.app.Activity;
import android.os.Bundle;
import com.service.bluetooth.BluetoothManager;
import com.service.bluetooth.BluetoothManager.ConnectListener;
import com.service.bluetooth.IBluetoothManager;
import com.service.bluetooth.WLog;

public class MainActivity extends Activity
{
    /**
     * TAG
     */
    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        WLog.d(TAG, "onCreate...");
        setContentView(R.layout.activity_main);
        final BluetoothManager manager = BluetoothManager.getDefault(this);
        manager.registerListener(new ConnectListener()
        {
            @Override
            public void onServiceConnected()
            {
                WLog.d(TAG, "onServiceConnected...");
                IBluetoothManager service = manager.getService();
                WLog.d(TAG, "service = " + service);
                boolean isEnable = manager.isEnabled();
                WLog.d(TAG, "isEnable = " + isEnable);
            }

            @Override
            public void onServiceDisConnected()
            {
                WLog.d(TAG, "onServiceDisConnected...");
            }
        });
//        try
//        {
//            Thread.sleep(2000);
//        }
//        catch (InterruptedException e)
//        {
//            e.printStackTrace();
//        }
//        if (manager != null)
        {
//            boolean isEnable = manager.isEnabled();
//            WLog.d(TAG, "isEnable = " + isEnable);
        }
    }
}
