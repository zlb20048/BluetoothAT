package com.service.bluetooth;

import java.util.*;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * <b>API is under Development may change in future release.</b>
 * <p/>
 * <P>Represents the local device Bluetooth adapter. The {@link }
 * lets you perform fundamental Bluetooth tasks, such as initiate
 * device discovery, query a list of bonded (paired) devices,
 * instantiate a {@link BluetoothDevice} using a known MAC address.</p>
 * <p/>
 * <p>To get a {@link } representing the local Bluetooth
 * adapter, call the static {@link #getDefault(android.content.Context)} method.
 * Fundamentally, this is your starting point for all
 * Bluetooth actions. Once you have the local adapter, you can get a set of
 * {@link BluetoothDevice} objects representing all paired devices with
 * {@link #getBondedDevices()}; start device discovery with
 * {@link #startDiscovery()}.
 * <p/>
 * <p class="note"><strong>Note:</strong>
 * Most methods require the {@link android.Manifest.permission#BLUETOOTH}
 * permission and some also require the
 * {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
 */
public class BluetoothManager
{

    private static final String TAG = "BTManager";
    private static final boolean DBG = true;
    private static final int CALL_STATE_INCOMING = 1;
    private static final int CALL_STATE_INCALL = 2;
    private static final int CALL_STATE_OUTGOING = 3;

    /**
     * Sentinel error value for this class. Guaranteed to not equal any other
     * integer constant in this class. Provided as a convenience for functions
     * that require a sentinel error value, for example:
     * <p><code>Intent.getIntExtra(.EXTRA_STATE, .ERROR)</code>
     */
    public static final int ERROR = Integer.MIN_VALUE;

    /**
     * Broadcast Action: The state of the local Bluetooth adapter has been
     * changed.
     * <p>For example, Bluetooth has been turned on or off.
     * <p>Always contains the extra fields {@link #EXTRA_STATE} and
     * {@link #EXTRA_PREVIOUS_STATE} containing the new and old states
     * respectively.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_STATE_CHANGED = "chleon.android.bluetooth.adapter.action.STATE_CHANGED";

    /**
     * Used as an int extra field in {@link #ACTION_STATE_CHANGED}
     * intents to request the current power state. Possible values are:
     * {@link #STATE_OFF},
     * {@link #STATE_TURNING_ON},
     * {@link #STATE_ON},
     * {@link #STATE_TURNING_OFF},
     */
    public static final String EXTRA_STATE = "android.bluetooth.adapter.extra.STATE";
    /**
     * Used as an int extra field in {@link #ACTION_STATE_CHANGED}
     * intents to request the previous power state. Possible values are:
     * {@link #STATE_OFF},
     * {@link #STATE_TURNING_ON},
     * {@link #STATE_ON},
     * {@link #STATE_TURNING_OFF},
     */
    public static final String EXTRA_PREVIOUS_STATE = "android.bluetooth.adapter.extra.PREVIOUS_STATE";

    /**
     * Indicates the local Bluetooth adapter is off.
     */
    public static final int STATE_OFF = 10;
    /**
     * Indicates the local Bluetooth adapter is turning on. However local
     * clients should wait for {@link #STATE_ON} before attempting to
     * use the adapter.
     */
    public static final int STATE_TURNING_ON = 11;
    /**
     * Indicates the local Bluetooth adapter is on, and ready for use.
     */
    public static final int STATE_ON = 12;
    /**
     * Indicates the local Bluetooth adapter is turning off. Local clients
     * should immediately attempt graceful disconnection of any remote links.
     */
    public static final int STATE_TURNING_OFF = 13;

    /**
     * Activity Action: Show a system activity that requests discoverable mode.
     * This activity will also request the user to turn on Bluetooth if it
     * is not currently enabled.
     * <p>Discoverable mode is equivalent to
     * {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE}. It allows remote devices to see
     * this Bluetooth adapter when they perform a discovery.
     * <p>For privacy, Android is not discoverable by default.
     * <p>The sender of this Intent can optionally use extra field
     * {@link #EXTRA_DISCOVERABLE_DURATION} to request the duration of
     * discoverability. Currently the default duration is 120 seconds, and
     * maximum duration is capped at 300 seconds for each request.
     * <p>Notification of the result of this activity is posted using the
     * {@link android.app.Activity#onActivityResult} callback. The
     * <code>resultCode</code>
     * will be the duration (in seconds) of discoverability or
     * {@link android.app.Activity#RESULT_CANCELED} if the user rejected
     * discoverability or an error has occurred.
     * <p>Applications can also listen for {@link #ACTION_SCAN_MODE_CHANGED}
     * for global notification whenever the scan mode changes. For example, an
     * application can be notified when the device has ended discoverability.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
     */
    //    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_DISCOVERABLE = "android.bluetooth.adapter.action.REQUEST_DISCOVERABLE";

    /**
     * Used as an optional int extra field in
     * {@link #ACTION_REQUEST_DISCOVERABLE} intents to request a specific duration
     * for discoverability in seconds. The current default is 120 seconds, and
     * requests over 300 seconds will be capped. These values could change.
     */
    public static final String EXTRA_DISCOVERABLE_DURATION = "android.bluetooth.adapter.extra.DISCOVERABLE_DURATION";

    /**
     * Activity Action: Show a system activity that allows the user to turn on
     * Bluetooth.
     * <p>This system activity will return once Bluetooth has completed turning
     * on, or the user has decided not to turn Bluetooth on.
     * <p>Notification of the result of this activity is posted using the
     * {@link android.app.Activity#onActivityResult} callback. The
     * <code>resultCode</code>
     * will be {@link android.app.Activity#RESULT_OK} if Bluetooth has been
     * turned on or {@link android.app.Activity#RESULT_CANCELED} if the user
     * has rejected the request or an error has occurred.
     * <p>Applications can also listen for {@link #ACTION_STATE_CHANGED}
     * for global notification whenever Bluetooth is turned on or off.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
     */
    //    @SdkConstant(SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_REQUEST_ENABLE = "android.bluetooth.adapter.action.REQUEST_ENABLE";

    /**
     * Broadcast Action: Indicates the Bluetooth scan mode of the local Adapter
     * has changed.
     * <p>Always contains the extra fields {@link #EXTRA_SCAN_MODE} and
     * {@link #EXTRA_PREVIOUS_SCAN_MODE} containing the new and old scan modes
     * respectively.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_SCAN_MODE_CHANGED = "android.bluetooth.adapter.action.SCAN_MODE_CHANGED";

    /**
     * Used as an int extra field in {@link #ACTION_SCAN_MODE_CHANGED}
     * intents to request the current scan mode. Possible values are:
     * {@link #SCAN_MODE_NONE},
     * {@link #SCAN_MODE_CONNECTABLE},
     * {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE},
     */
    public static final String EXTRA_SCAN_MODE = "android.bluetooth.adapter.extra.SCAN_MODE";
    /**
     * Used as an int extra field in {@link #ACTION_SCAN_MODE_CHANGED}
     * intents to request the previous scan mode. Possible values are:
     * {@link #SCAN_MODE_NONE},
     * {@link #SCAN_MODE_CONNECTABLE},
     * {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE},
     */
    public static final String EXTRA_PREVIOUS_SCAN_MODE = "android.bluetooth.adapter.extra.PREVIOUS_SCAN_MODE";

    /**
     * Indicates that both inquiry scan and page scan are disabled on the local
     * Bluetooth adapter. Therefore this device is neither discoverable
     * nor connectable from remote Bluetooth devices.
     */
    public static final int SCAN_MODE_NONE = 20;
    /**
     * Indicates that inquiry scan is disabled, but page scan is enabled on the
     * local Bluetooth adapter. Therefore this device is not discoverable from
     * remote Bluetooth devices, but is connectable from remote devices that
     * have previously discovered this device.
     */
    public static final int SCAN_MODE_CONNECTABLE = 21;
    /**
     * Indicates that both inquiry scan and page scan are enabled on the local
     * Bluetooth adapter. Therefore this device is both discoverable and
     * connectable from remote Bluetooth devices.
     */
    public static final int SCAN_MODE_CONNECTABLE_DISCOVERABLE = 23;


    /**
     * Broadcast Action: The local Bluetooth adapter has started the remote
     * device discovery process.
     * <p>This usually involves an inquiry scan of about 12 seconds, followed
     * by a page scan of each new device to retrieve its Bluetooth name.
     * <p>Register for {@link BluetoothDevice#ACTION_FOUND} to be notified as
     * remote Bluetooth devices are found.
     * <p>Device discovery is a heavyweight procedure. New connections to
     * remote Bluetooth devices should not be attempted while discovery is in
     * progress, and existing connections will experience limited bandwidth
     * and high latency. Use {@link #cancelDiscovery()} to cancel an ongoing
     * discovery.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_DISCOVERY_STARTED = "android.bluetooth.adapter.action.DISCOVERY_STARTED";
    /**
     * Broadcast Action: The local Bluetooth adapter has finished the device
     * discovery process.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_DISCOVERY_FINISHED = "android.bluetooth.adapter.action.DISCOVERY_FINISHED";

    /**
     * Broadcast Action: The local Bluetooth adapter has changed its friendly
     * Bluetooth name.
     * <p>This name is visible to remote Bluetooth devices.
     * <p>Always contains the extra field {@link #EXTRA_LOCAL_NAME} containing
     * the name.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_LOCAL_NAME_CHANGED = "android.bluetooth.adapter.action.LOCAL_NAME_CHANGED";
    /**
     * Used as a String extra field in {@link #ACTION_LOCAL_NAME_CHANGED}
     * intents to request the local Bluetooth name.
     */
    public static final String EXTRA_LOCAL_NAME = "android.bluetooth.adapter.extra.LOCAL_NAME";

    /**
     * @hide
     */
    public static final String BLUETOOTH_SERVICE = "bluetooth";

    private static final int ADDRESS_LENGTH = 17;
    private static BluetoothManager sManager;
    public static IBluetoothManager mService;
    private final static String SERVICE_FILTER = "com.service.bluetooth.ATService";
    private static ConnectListener listener;

    /**
     * Get a handle to the default local Bluetooth manager.
     * <p>Currently Android only supports one Bluetooth adapter, but the API
     * could be extended to support more. This will always return the default
     * adapter.
     *
     * @param context
     * @return the default local manager, or null if Bluetooth is not supported
     * on this hardware platform
     */
    public static synchronized BluetoothManager getDefault(Context context)
    {
        if (sManager == null)
        {
            if (null != context)
            {
                Intent serviceIntent = new Intent(SERVICE_FILTER);
                WLog.d(TAG, "bindService is called");
                boolean isBind = context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                WLog.d(TAG, "isBind = " + isBind);
                if (!isBind)
                {
                    WLog.d(TAG, "can not bind to AT Service..");
                }
            }
            sManager = new BluetoothManager();
        }
        return sManager;
    }

    public void registerListener(ConnectListener listener)
    {
        this.listener = listener;
    }

    public interface ConnectListener
    {
        void onServiceConnected();

        void onServiceDisConnected();
    }

    private static ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder)
        {
            WLog.d(TAG, "onServiceConnected...");
            mService = IBluetoothManager.Stub.asInterface(iBinder);
//            WLog.d(TAG, "iBluetoothManager  = " + iBluetoothManager);
//            sManager = new BluetoothManager(iBluetoothManager);
            if (null != listener)
            {
                listener.onServiceConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            WLog.d(TAG, "onServiceDisconnected...");
            sManager = null;
            if (null != listener)
            {
                listener.onServiceDisConnected();
            }
        }
    };

    private BluetoothManager()
    {
    }

    /**
     * Use {@link #getDefault(android.content.Context)} to get the BluetoothAdapter instance.
     *
     * @hide
     */
    private BluetoothManager(IBluetoothManager service)
    {
        if (service == null)
        {
            throw new IllegalArgumentException("service is null");
        }
        mService = service;
    }

    public IBluetoothManager getService()
    {
        return mService;
    }

    /**
     * Get a {@link BluetoothDevice} object for the given Bluetooth hardware
     * address.
     * <p>Valid Bluetooth hardware addresses must be upper case, in a format
     * such as "00:11:22:33:AA:BB". The helper {@link #checkBluetoothAddress(String)} is
     * available to validate a Bluetooth address.
     * <p>A {@link BluetoothDevice} will always be returned for a valid
     * hardware address, even if this adapter has never seen that device.
     *
     * @param address valid Bluetooth MAC address
     * @throws IllegalArgumentException if address is invalid
     */
    public BluetoothDevice getRemoteDevice(String address)
    {
        return new BluetoothDevice(address);
    }

    /**
     * Return true if Bluetooth is currently enabled and ready for use.
     * <p>Equivalent to:
     * <code>getState() == .STATE_ON</code>
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
     *
     * @return true if the local adapter is turned on
     */
    public boolean isEnabled()
    {
        try
        {
            return mService.isEnabled();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /*
     */
    public boolean bluetooth_enable(boolean on)
    {
        try
        {
            return mService.bluetooth_enable(on);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }


    /**
     * Turn on the local Bluetooth adapter&mdash;do not use without explicit
     * user action to turn on Bluetooth.
     * <p>This powers on the underlying Bluetooth hardware, and starts all
     * Bluetooth system services.
     * <p class="caution"><strong>Bluetooth should never be enabled without
     * direct user consent</strong>. If you want to turn on Bluetooth in order
     * to create a wireless connection, you should use the
     * {@link #ACTION_REQUEST_ENABLE} Intent, which will raise a dialog that requests
     * user permission to turn on Bluetooth. The {@link #enable()} method is
     * provided only for applications that include a user interface for changing
     * system settings, such as a "power manager" app.</p>
     * <p>This is an asynchronous call: it will return immediately, and
     * clients should listen for {@link #ACTION_STATE_CHANGED}
     * to be notified of subsequent adapter state changes. If this call returns
     * true, then the adapter state will immediately transition from
     * {@link #STATE_OFF} to {@link #STATE_TURNING_ON}, and some time
     * later transition to either {@link #STATE_OFF} or
     * {@link #STATE_ON}. If this call returns false then there was an
     * immediate problem that will prevent the adapter from being turned on -
     * such as Airplane mode, or the adapter is already turned on.
     * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
     * permission
     *
     * @return true to indicate adapter startup has begun, or false on
     * immediate error
     */
    public boolean enable()
    {
        try
        {
            return mService.enable();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Turn off the local Bluetooth adapter&mdash;do not use without explicit
     * user action to turn off Bluetooth.
     * <p>This gracefully shuts down all Bluetooth connections, stops Bluetooth
     * system services, and powers down the underlying Bluetooth hardware.
     * <p class="caution"><strong>Bluetooth should never be disabled without
     * direct user consent</strong>. The {@link #disable(boolean)} method is
     * provided only for applications that include a user interface for changing
     * system settings, such as a "power manager" app.</p>
     * <p>This is an asynchronous call: it will return immediately, and
     * clients should listen for {@link #ACTION_STATE_CHANGED}
     * to be notified of subsequent adapter state changes. If this call returns
     * true, then the adapter state will immediately transition from
     * {@link #STATE_ON} to {@link #STATE_TURNING_OFF}, and some time
     * later transition to either {@link #STATE_OFF} or
     * {@link #STATE_ON}. If this call returns false then there was an
     * immediate problem that will prevent the adapter from being turned off -
     * such as the adapter already being turned off.
     * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
     * permission
     *
     * @return true to indicate adapter shutdown has begun, or false on
     * immediate error
     */
    public boolean disable(boolean persistSetting)
    {
        try
        {
            return mService.disable(true);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Get the current state of the local Bluetooth adapter.
     * <p>Possible return values are
     * {@link #STATE_OFF},
     * {@link #STATE_TURNING_ON},
     * {@link #STATE_ON},
     * {@link #STATE_TURNING_OFF}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
     *
     * @return current state of Bluetooth adapter
     */
    public int getState()
    {
        try
        {
            return mService.getBluetoothState();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return STATE_OFF;
    }

    /**
     * Get the friendly Bluetooth name of the local Bluetooth adapter.
     * <p>This name is visible to remote Bluetooth devices.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
     *
     * @return the Bluetooth name, or null on error
     */
    public String getName()
    {
        try
        {
            return mService.getName();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return null;
    }

    /**
     * Set the friendly Bluetooth name of the local Bluetooth adapter.
     * <p>This name is visible to remote Bluetooth devices.
     * <p>Valid Bluetooth names are a maximum of 248 bytes using UTF-8
     * encoding, although many remote devices can only display the first
     * 40 characters, and some may be limited to just 20.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}
     *
     * @param name a valid Bluetooth name
     * @return true if the name was set, false otherwise
     */
    public boolean setName(String name)
    {
        if (getState() != STATE_ON)
        {
            return false;
        }
        try
        {
            return mService.setName(name);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Returns the hardware address of the local Bluetooth manager.
     * <p>For example, "00:11:22:AA:BB:CC".
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
     *
     * @return Bluetooth hardware address as string
     */
    public String getAddress()
    {
        try
        {
            return mService.getAddress();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return null;
    }

    /**
     * Get the current Bluetooth scan mode of the local Bluetooth manager.
     * <p>The Bluetooth scan mode determines if the local adapter is
     * connectable and/or discoverable from remote Bluetooth devices.
     * <p>Possible values are:
     * {@link #SCAN_MODE_NONE},
     * {@link #SCAN_MODE_CONNECTABLE},
     * {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE}.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return {@link #SCAN_MODE_NONE}. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
     *
     * @return scan mode
     */
    public int getScanMode()
    {
        if (getState() != STATE_ON)
        {
            return SCAN_MODE_NONE;
        }
        try
        {
            return mService.getScanMode();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return SCAN_MODE_NONE;
    }

    /**
     * Set the Bluetooth scan mode of the local Bluetooth manager.
     * <p>The Bluetooth scan mode determines if the local adapter is
     * connectable and/or discoverable from remote Bluetooth devices.
     * <p>For privacy reasons, discoverable mode is automatically turned off
     * after <code>duration</code> seconds. For example, 120 seconds should be
     * enough for a remote device to initiate and complete its discovery
     * process.
     * <p>Valid scan mode values are:
     * {@link #SCAN_MODE_NONE},
     * {@link #SCAN_MODE_CONNECTABLE},
     * {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE}.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     * <p>Requires {@link android.Manifest.permission#WRITE_SECURE_SETTINGS}
     * <p>Applications cannot set the scan mode. They should use
     * <code>startActivityForResult(.ACTION_REQUEST_DISCOVERABLE})
     * </code>instead.
     *
     * @param mode     valid scan mode
     * @param duration time in seconds to apply scan mode, only used for
     *                 {@link #SCAN_MODE_CONNECTABLE_DISCOVERABLE}
     * @return true if the scan mode was set, false otherwise
     * @hide
     */
    public boolean setScanMode(int mode, int duration)
    {
        if (getState() != STATE_ON)
        {
            return false;
        }
        try
        {
            return mService.setScanMode(mode, duration);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }


    /**
     * Start the remote device discovery process.
     * <p>The discovery process usually involves an inquiry scan of about 12
     * seconds, followed by a page scan of each new device to retrieve its
     * Bluetooth name.
     * <p>This is an asynchronous call, it will return immediately. Register
     * for {@link #ACTION_DISCOVERY_STARTED} and
     * {@link #ACTION_DISCOVERY_FINISHED} intents to determine exactly when the
     * discovery starts and completes. Register for
     * {@link BluetoothDevice#ACTION_FOUND} to be notified as remote Bluetooth devices
     * are found.
     * <p>Device discovery is a heavyweight procedure. New connections to
     * remote Bluetooth devices should not be attempted while discovery is in
     * progress, and existing connections will experience limited bandwidth
     * and high latency. Use {@link #cancelDiscovery()} to cancel an ongoing
     * discovery. Discovery is not managed by the Activity,
     * but is run as a system service, so an application should always call
     * {@link #cancelDiscovery()} even if it
     * did not directly request a discovery, just to be sure.
     * <p>Device discovery will only find remote devices that are currently
     * <i>discoverable</i> (inquiry scan enabled). Many Bluetooth devices are
     * not discoverable by default, and need to be entered into a special mode.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}.
     *
     * @return true on success, false on error
     */
    public boolean startDiscovery()
    {
        if (getState() != STATE_ON)
        {
            return false;
        }
        try
        {
            return mService.startDiscovery();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Cancel the current device discovery process.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}.
     * <p>Because discovery is a heavyweight procedure for the Bluetooth
     * adapter, this method should always be called before attempting to connect
     * to a remote device with
     * {@link android.bluetooth.BluetoothSocket#connect()}. Discovery is not managed by
     * the  Activity, but is run as a system service, so an application should
     * always call cancel discovery even if it did not directly request a
     * discovery, just to be sure.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     *
     * @return true on success, false on error
     */
    public boolean cancelDiscovery()
    {
        if (getState() != STATE_ON)
        {
            return false;
        }
        try
        {
            mService.cancelDiscovery();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Return true if the local Bluetooth manager is currently in the device
     * discovery process.
     * <p>Device discovery is a heavyweight procedure. New connections to
     * remote Bluetooth devices should not be attempted while discovery is in
     * progress, and existing connections will experience limited bandwidth
     * and high latency. Use {@link #cancelDiscovery()} to cancel an ongoing
     * discovery.
     * <p>Applications can also register for {@link #ACTION_DISCOVERY_STARTED}
     * or {@link #ACTION_DISCOVERY_FINISHED} to be notified when discovery
     * starts or completes.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return false. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}.
     *
     * @return true if discovering
     */
    public boolean isDiscovering()
    {
        if (getState() != STATE_ON)
        {
            return false;
        }
        try
        {
            return mService.isDiscovering();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Return the set of {@link BluetoothDevice} objects that are bonded
     * (paired) to the local adapter.
     * <p>If Bluetooth state is not {@link #STATE_ON}, this API
     * will return an empty set. After turning on Bluetooth,
     * wait for {@link #ACTION_STATE_CHANGED} with {@link #STATE_ON}
     * to get the updated value.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}.
     *
     * @return unmodifiable set of {@link BluetoothDevice}, or null on error
     */
    public Set<BluetoothDevice> getBondedDevices()
    {
        if (getState() != STATE_ON)
        {
            return toDeviceSet(new String[0]);
        }
        try
        {
            return toDeviceSet(mService.getBondedDevices());
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        catch (IllegalArgumentException ie)
        {
            WLog.e(TAG, "", ie);
        }
        return null;
    }

    public int getMaxBondedDeviceCount()
    {
        try
        {
            return mService.getMaxBondedDeviceCount();
        }
        catch (RemoteException e)
        {
        }
        return 0;
    }

    private Set<BluetoothDevice> toDeviceSet(String[] addresses)
    {
        Set<BluetoothDevice> devices = new HashSet<BluetoothDevice>(addresses.length);
        for (int i = 0; i < addresses.length; i++)
        {
            devices.add(getRemoteDevice(addresses[i]));
        }
        return Collections.unmodifiableSet(devices);
    }

    /**
     * Validate a Bluetooth address, such as "00:43:A8:23:10:F0"
     * <p>Alphabetic characters must be uppercase to be valid.
     *
     * @param address Bluetooth address as string
     * @return true if the address is valid, false otherwise
     */
    public static boolean checkBluetoothAddress(String address)
    {
        if (address == null || address.length() != ADDRESS_LENGTH)
        {
            return false;
        }
        for (int i = 0; i < ADDRESS_LENGTH; i++)
        {
            char c = address.charAt(i);
            switch (i % 3)
            {
                case 0:
                case 1:
                    if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'))
                    {
                        // hex character, OK
                        break;
                    }
                    return false;
                case 2:
                    if (c == ':')
                    {
                        break;  // OK
                    }
                    return false;
            }
        }
        return true;
    }

    public int getPhonebookSize(String address)
    {
        try
        {
            return mService.getPhonebookSize(address);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return -1;
    }

    /**
     *
     */
    public int getPhonePrivateMode()
    {
        try
        {
            return mService.getPhonePrivateMode();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return 1;
    }

    /**
     *
     */
    public boolean setPhonePrivateMode(int mode)
    {
        try
        {
            return mService.setPhonePrivateMode(mode);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     *
     */
    public int getAutoConnMode()
    {
        try
        {
            return mService.getAutoConnMode();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return 1;
    }

    /**
     *
     */
    public boolean setAutoConnMode(int mode)
    {
        try
        {
            return mService.setAutoConnMode(mode);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }


    /**
     *
     */
    public int getMicMuteState()
    {
        try
        {
            return mService.getMicMuteState();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return 1;
    }

    /**
     *
     */
    public boolean setMicMuteState(int unMuted)
    {
        try
        {
            return mService.setMicMuteState(unMuted);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     *
     */
    public boolean generateDTMF(char value)
    {
        try
        {
            return mService.generateDTMF(value);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     *
     */
    public boolean switchCalls()
    {
        try
        {
            return mService.switchCalls();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }


    /**
     *
     */
    public boolean setStartPBSyncManual(String address)
    {
        try
        {
            return mService.setStartPBSyncManual(address);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     *
     */
    public boolean setADCConfiguration(int type, int value)
    {
        try
        {
            return mService.setADCConfiguration(type, value);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }


    /**
     *
     */
    public boolean setAudioVolume(int type, int value)
    {
        try
        {
            return mService.setAudioVolume(type, value);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     *
     */
    public int getAudioVolume(int type)
    {
        try
        {
            return mService.getAudioVolume(type);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return 0;
    }


    /**
     *
     */
    public int[] getAudioVolumeRange(int type)
    {
        try
        {
            return mService.getAudioVolumeRange(type);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return null;
    }


    /**
     *
     */
    public void sendCommand(String cmd)
    {
        try
        {
            mService.sendCommand(cmd);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return;
    }

    /**
     *
     */
    public List<BluetoothMessage> getBtMessage(String address)
    {
        try
        {
            return mService.getBtMessage(address);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return null;
    }


    /**
     *
     */
    public int getMapSvcState(String address)
    {
        try
        {
            return mService.getMapSvcState(address);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return -1;
    }


    /**
     *
     */
    public int getPhoneSvcState(String address)
    {
        try
        {
            return mService.getPhoneSvcState(address);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return -1;
    }

    /**
     *
     */
    public int getA2DPSinkSvcState(String address)
    {
        try
        {
            return mService.getA2DPSinkSvcState(address);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return -1;
    }


    /**
     *
     */
    public boolean isMapMsgDownloading(String address)
    {
        try
        {
            return mService.isMapMsgDownloading(address);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     *
     */
    public boolean retriveMapMessage(String address, int accountId, int msgId)
    {
        try
        {
            return mService.retriveMapMessage(address, accountId, msgId);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }


    /**
     *
     */
    public boolean sendMapMessage(String address, BluetoothMessage msg)
    {
        try
        {
            return mService.sendMapMessage(address, msg);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     *
     */
    public String GetConnectDeviceAddr(int profile)
    {
        try
        {
            return mService.GetConnectDeviceAddr(profile);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return null;
    }


    /*
     */
    public String getConnectDeviceAddr(int profile)
    {
        try
        {
            return mService.getConnectDeviceAddr(profile);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return null;
    }


    /**
     *
     */
    public boolean deviceConnect(String address, int profile, boolean state)
    {
        try
        {
            return mService.deviceConnect(address, profile, state);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }


    /**
     *
     */
    public int getDeviceServiceState(String address, int profile)
    {
        try
        {
            return mService.getDeviceServiceState(address, profile);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return 0;
    }


    public String getSwVersion()
    {
        try
        {
            return mService.getSwVersion();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return null;
    }

    public boolean enableUpdateMode()
    {
        try
        {
            return mService.enableUpdateMode();
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    public void ttsSpeak(String text)
    {
        try
        {
            mService.ttsSpeak(text);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return;
    }

    /**
     * Register listener for Bluetooth.
     *
     * @param listener, refer to TBoxListener for more info
     * @param events,   event apps care about
     */

    public void listen(BluetoothListener listener, int events)
    {
        String pkgForDebug = "<unknown>";
        try
        {
            boolean notifyNow = true;
            mService.listen(pkgForDebug, listener.callback, events, notifyNow);
        }
        catch (RemoteException ex)
        {
            // system process dead
        }
        catch (NullPointerException ex)
        {
            // system process dead
        }
    }

    public boolean isIdle()
    {
        Set<BluetoothDevice> sBluetoothDevices = getBondedDevices();
        if ((sBluetoothDevices == null) || (sBluetoothDevices.size() <= 0))
        {
            WLog.d(TAG, "reloadBondedDevices, get no device");
            return true;
        }
        BluetoothDevice device = null;
        for (BluetoothDevice dev : sBluetoothDevices)
        {
            device = dev;
            if (device.getState() == BluetoothDevice.CONNECT_CONNECTED)
            {
                break;
            }
        }
        if (device == null)
        {
            return true;
        }
        int callState = device.getCallState();
        switch (callState)
        {
            case CALL_STATE_OUTGOING:
            case CALL_STATE_INCOMING:
            case CALL_STATE_INCALL:
                return false;
        }
        return true;
    }
}

