package com.service.bluetooth;

import android.app.Application;
import android.app.SearchableInfo;
import android.os.RemoteException;
import android.test.ApplicationTestCase;

import java.util.TreeMap;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application>
{

    private final static String TAG = ApplicationTest.class.getSimpleName();

    private BtManagerService service = null;

    public ApplicationTest()
    {
        super(Application.class);
        WLog.v("ApplicationTest create");
    }

    public void testEnable() throws InterruptedException
    {
        WLog.d(TAG, "testEnable");
        service = new BtManagerService(getContext());
        service.setCommandInterface(new BluetoothInterfaceLayer(getContext()));
        Thread.sleep(500);

        boolean isEnable = service.isEnabled();
        Thread.sleep(1000);
        WLog.d(TAG, "isEnable = " + isEnable);

        service.enable();
        Thread.sleep(1000);

        isEnable = service.isEnabled();
        Thread.sleep(1000);
        WLog.d(TAG, "isEnable = " + isEnable);

        String[] bondDevices = service.getBondedDevices();
        for (String address : bondDevices)
        {
            WLog.d(TAG, "address = " + address);
        }
        Thread.sleep(1000);

        service.setScanMode(BluetoothManager.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 8);
        Thread.sleep(1000);

        String name = service.getName();
        Thread.sleep(100);
        WLog.d(TAG, "name = " + name);

//        service.startDiscovery();
//        Thread.sleep(10 * 1000);

        //        service.sendCommand("AT+B PAIR 38:71:DE:D3:A2:73");
//        service.sendCommand("AT+B PAIR 3871DED3A273");
//        service.createBond("38:71:DE:D3:A2:73");
        try
        {
            service.setDeviceConnected("38:71:DE:D3:A2:73", true);
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
        Thread.sleep(60 * 1000);


//        service.getPhoneBookByManual("38:71:DE:D3:A2:73");
//        Thread.sleep(60 * 1000);

//        service.call("10086");
//        Thread.sleep(10 * 1000);
//
//        service.hangUpCall();
//        Thread.sleep(1000);
    }
}