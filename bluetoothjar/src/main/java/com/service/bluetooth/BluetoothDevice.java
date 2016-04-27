package com.service.bluetooth;

import java.io.UnsupportedEncodingException;

import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.RemoteException;

/**
 * <b>API is under Development may change in future release.</b>
 * <p>
 * Represents a remote Bluetooth device. A {@link com.service.bluetooth.BluetoothDevice} lets you
 * create a connection with the respective device or query information about
 * it, such as the name, address, class, and bonding state.</p>
 * <p/>
 * <p>This class is really just a thin wrapper for a Bluetooth hardware
 * address. Objects of this class are immutable. Operations on this class
 * are performed on the remote Bluetooth hardware address, using the
 * {@link BluetoothManager} that was used to create this {@link com.service.bluetooth.BluetoothDevice}.
 * <p/>
 * <p>To get a {@link com.service.bluetooth.BluetoothDevice}, use
 * {@link BluetoothManager#getRemoteDevice(String) BluetoothManager.getRemoteDevice(String)}
 * to create one representing a device
 * of a known MAC address (which you can get through device discovery with
 * {@link BluetoothManager}) or get one from the set of bonded devices
 * returned by {@link BluetoothManager#getBondedDevices() BluetoothManager.getBondedDevices()}.
 * <p/>
 * <p class="note"><strong>Note:</strong>
 * Requires the {@link android.Manifest.permission#BLUETOOTH} permission.
 */
public final class BluetoothDevice implements Parcelable
{
    private static final String TAG = "BluetoothDevice";

    /**
     * Sentinel error value for this class. Guaranteed to not equal any other
     * integer constant in this class. Provided as a convenience for functions
     * that require a sentinel error value, for example:
     * <p><code>Intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
     * BluetoothDevice.ERROR)</code>
     */
    public static final int ERROR = Integer.MIN_VALUE;

    /**
     * Broadcast Action: Remote device discovered.
     * <p>Sent when a remote device is found during discovery.
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and
     * {@link #EXTRA_CLASS}. Can contain the extra fields {@link #EXTRA_NAME} and/or
     * {@link #EXTRA_RSSI} if they are available.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    // TODO: Change API to not broadcast RSSI if not available (incoming connection)
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    //TODO remove chleon after including in build
    public static final String ACTION_FOUND = "chleon.android.bluetooth.device.action.FOUND";

    /**
     * Broadcast Action: Remote device disappeared.
     * <p>Sent when a remote device that was found in the last discovery is not
     * found in the current discovery.
     * <p>Always contains the extra field {@link #EXTRA_DEVICE}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     *
     * @hide
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_DISAPPEARED = "android.bluetooth.device.action.DISAPPEARED";

    /**
     * Broadcast Action: Bluetooth class of a remote device has changed.
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link
     * #EXTRA_CLASS}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_CLASS_CHANGED = "android.bluetooth.device.action.CLASS_CHANGED";

    /**
     * Broadcast Action: Indicates a low level (ACL) connection has been
     * established with a remote device.
     * <p>Always contains the extra field {@link #EXTRA_DEVICE}.
     * <p>ACL connections are managed automatically by the Android Bluetooth
     * stack.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ACL_CONNECTED = "android.bluetooth.device.action.ACL_CONNECTED";

    /**
     * Broadcast Action: Indicates that a low level (ACL) disconnection has
     * been requested for a remote device, and it will soon be disconnected.
     * <p>This is useful for graceful disconnection. Applications should use
     * this intent as a hint to immediately terminate higher level connections
     * (RFCOMM, L2CAP, or profile connections) to the remote device.
     * <p>Always contains the extra field {@link #EXTRA_DEVICE}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ACL_DISCONNECT_REQUESTED = "android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED";

    /**
     * Broadcast Action: Indicates a low level (ACL) disconnection from a
     * remote device.
     * <p>Always contains the extra field {@link #EXTRA_DEVICE}.
     * <p>ACL connections are managed automatically by the Android Bluetooth
     * stack.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ACL_DISCONNECTED = "android.bluetooth.device.action.ACL_DISCONNECTED";

    /**
     * Broadcast Action: Indicates the friendly name of a remote device has
     * been retrieved for the first time, or changed since the last retrieval.
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE} and {@link
     * #EXTRA_NAME}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_NAME_CHANGED = "android.bluetooth.device.action.NAME_CHANGED";

    /**
     * Broadcast Action: Indicates a change in the bond state of a remote
     * device. For example, if a device is bonded (paired).
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE}, {@link
     * #EXTRA_BOND_STATE} and {@link #EXTRA_PREVIOUS_BOND_STATE}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    // Note: When EXTRA_BOND_STATE is BOND_NONE then this will also
    // contain a hidden extra field EXTRA_REASON with the result code.
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_BOND_STATE_CHANGED = "chleon.android.bluetooth.device.action.BOND_STATE_CHANGED";

    /**
     * Broadcast Action: Indicates a change in the connected state of a remote
     * device. For example, if a device is already paired and connected/Disconnected.
     * <p>Always contains the extra fields {@link #EXTRA_DEVICE},
     * {@link #EXTRA_STATE} and {@link #EXTRA_PREVIOUS_STATE}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    // Note: When EXTRA_BOND_STATE is BOND_NONE then this will also
    // contain a hidden extra field EXTRA_REASON with the result code.
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_STATE_CHANGED = "chleon.android.bluetooth.device.action.STATE_CHANGED";

    /**
     * Broadcast Action: Indicates a change in the play, pause, stop
     * mode of device player. For example, if a device is bonded (paired).
     * and the player is in play mode once stopped , it will send a broadcast
     * of updated status in {@link #EXTRA_PLAYER_STATUS}.
     */
    public static final String ACTION_PLAYER_STATUS = "chleon.android.bluetooth.device.action.player.status";


    public static final String ACTION_PLAYER_META_DATE_CHANGED = "chleon.android.bluetooth.device.action.player.metadate";
    /**
     * Used as a Parcelable {@link com.service.bluetooth.BluetoothDevice} extra field in every intent
     * broadcast by this class. It contains the {@link com.service.bluetooth.BluetoothDevice} that
     * the intent applies to.
     */
    public static final String EXTRA_DEVICE = "android.bluetooth.device.extra.DEVICE";
    /**
     * The lookup key used with the {@link #ACTION_PHONE_STATE_CHANGED} broadcast
     * for a String containing the new call state.
     *
     * @see #CONNECT_CONNECTING
     * @see #CONNECT_CONNECTED
     * @see #CONNECT_DISCONNECTING
     * @see #CONNECT_CONNECTED
     * <p class="note">
     * Retrieve with
     * {@link android.content.Intent#getStringExtra(String)}.
     */
    public static final String EXTRA_STATE = "android.bluetooth.device.extra.STATE";

    /**
     * Used as a String extra field in {@link #ACTION_NAME_CHANGED} and {@link
     * #ACTION_FOUND} intents. It contains the friendly Bluetooth name.
     */
    public static final String EXTRA_NAME = "android.bluetooth.device.extra.NAME";

    /**
     * Used as an optional short extra field in {@link #ACTION_FOUND} intents.
     * Contains the RSSI value of the remote device as reported by the
     * Bluetooth hardware.
     */
    public static final String EXTRA_RSSI = "android.bluetooth.device.extra.RSSI";

    /**
     * Used as an Parcelable extra field in
     * {@link #ACTION_FOUND} and {@link #ACTION_CLASS_CHANGED} intents.
     */
    public static final String EXTRA_CLASS = "android.bluetooth.device.extra.CLASS";

    /**
     * Used as an int extra field in {@link #ACTION_BOND_STATE_CHANGED} intents.
     * Contains the bond state of the remote device.
     * <p>Possible values are:
     * {@link #BOND_NONE},
     * {@link #BOND_BONDING},
     * {@link #BOND_BONDED}.
     */
    public static final String EXTRA_BOND_STATE = "android.bluetooth.device.extra.BOND_STATE";
    /**
     * Used as an int extra field in {@link #ACTION_BOND_STATE_CHANGED} intents.
     * Contains the previous bond state of the remote device.
     * <p>Possible values are:
     * {@link #BOND_NONE},
     * {@link #BOND_BONDING},
     * {@link #BOND_BONDED}.
     */
    public static final String EXTRA_PREVIOUS_BOND_STATE = "android.bluetooth.device.extra.PREVIOUS_BOND_STATE";

    /**
     * Used as an int extra field in {@link #ACTION_BOND_STATE_CHANGED} intents.
     * Contains the bond state of the remote device.
     * <p>Possible values are:
     * {@link #BOND_NONE},
     * {@link #BOND_BONDING},
     * {@link #BOND_BONDED}.
     */
    public static final String EXTRA_CURRENT_STATE = "android.bluetooth.device.extra.STATE";
    /**
     * Used as an int extra field in {@link #ACTION_BOND_STATE_CHANGED} intents.
     * Contains the previous bond state of the remote device.
     * <p>Possible values are:
     * {@link #BOND_NONE},
     * {@link #BOND_BONDING},
     * {@link #BOND_BONDED}.
     */
    public static final String EXTRA_PREVIOUS_STATE = "android.bluetooth.device.extra.PREVIOUS_STATE";

    /**
     * Used as a  extra field in every intent
     * broadcast by this class. It contains the state of Player.
     * the intent applies to.
     *
     * @see #PLAYER_STATUS_PAUSE
     * @see #PLAYER_STATUS_PLAY
     * @see #PLAYER_STATUS_STOP
     */
    public static final String EXTRA_PLAYER_STATUS = "android.bluetooth.device.extra.PLAYER_STATUS";

    public static final int PLAYER_STATUS_PLAY = 101;
    public static final int PLAYER_STATUS_PAUSE = 102;
    public static final int PLAYER_STATUS_STOP = 103;

    /**
     * Used as a extra field in every intent
     * broadcast by this class. It contains the {@link #getTittle() } that
     * the intent applies to.
     *
     * @see #PLAYER_STATUS_PAUSE
     * @see #PLAYER_STATUS_PLAY
     */
    public static final String EXTRA_PLAYER_TITTLE = "android.bluetooth.device.extra.PLAYER_TITTLE";

    /**
     * Indicates the remote device is not bonded (paired).
     * <p>There is no shared link key with the remote device, so communication
     * (if it is allowed at all) will be unauthenticated and unencrypted.
     */


    public static final int BOND_NONE = 10;
    /**
     * Indicates bonding (pairing) is in progress with the remote device.
     */
    public static final int BOND_BONDING = 11;
    /**
     * Indicates the remote device is bonded (paired).
     * <p>A shared link keys exists locally for the remote device, so
     * communication can be authenticated and encrypted.
     * <p><i>Being bonded (paired) with a remote device does not necessarily
     * mean the device is currently connected. It just means that the pending
     * procedure was completed at some earlier time, and the link key is still
     * stored locally, ready to use on the next connection.
     * </i>
     */
    public static final int BOND_BONDED = 12;

    /**
     * Indicates connection is in progress with the remote device.
     */
    public static final int CONNECT_CONNECTING = 30;
    /**
     * Indicates the remote device is Connected .
     * <p>A device is connected and active communication between host and remote
     * device is possible now
     * <p/>
     */
    public static final int CONNECT_CONNECTED = 31;
    /**
     * Indicates the remote device is disConnected .
     * <p>A device is disconnected and active communication between host and remote
     * device is possible now
     * <p/>
     */
    public static final int CONNECT_DISCONNECTED = 32;

    /**
     * Indicates disconnection is in progress with the remote device.
     */
    public static final int CONNECT_DISCONNECTING = 33;


    /**
     * @hide
     */
    public static final String EXTRA_REASON = "android.bluetooth.device.extra.REASON";
    /**
     * @hide
     */
    public static final String EXTRA_PAIRING_VARIANT = "android.bluetooth.device.extra.PAIRING_VARIANT";
    /**
     * @hide
     */
    public static final String EXTRA_PASSKEY = "android.bluetooth.device.extra.PASSKEY";

    /**
     * Broadcast Action: This intent is used to broadcast the {@link java.util.UUID}
     * wrapped as a {@link android.os.ParcelUuid} of the remote device after it
     * has been fetched. This intent is sent only when the UUIDs of the remote
     * device are requested to be fetched using Service Discovery Protocol
     * <p> Always contains the extra field {@link #EXTRA_DEVICE}
     * <p> Always contains the extra filed {@link #EXTRA_UUID}
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     *
     * @hide
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_UUID = "android.bleutooth.device.action.UUID";

    /**
     * Broadcast Action: Indicates a failure to retrieve the name of a remote
     * device.
     * <p>Always contains the extra field {@link #EXTRA_DEVICE}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     *
     * @hide
     */
    //TODO: is this actually useful?
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_NAME_FAILED = "android.bluetooth.device.action.NAME_FAILED";

    /**
     * @hide
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_PAIRING_REQUEST = "android.bluetooth.device.action.PAIRING_REQUEST";
    /**
     * @hide
     */
    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_PAIRING_CANCEL = "android.bluetooth.device.action.PAIRING_CANCEL";


    /**
     * Broadcast intent action indicating that the call state (cellular)
     * on the device has changed.
     * <p/>
     * <p/>
     * The {@link #EXTRA_PHONE_STATE} extra indicates the new call state.
     * If the new state is RINGING, a second extra
     * {@link #EXTRA_INCOMING_NUMBER} provides the incoming phone number as
     * a String.
     * <p/>
     * <p class="note">
     * Requires the READ_PHONE_STATE permission.
     * <p/>
     * <p class="note">
     * This was a {@link android.content.Context#sendStickyBroadcast sticky}
     * broadcast in version 1.0, but it is no longer sticky.
     * Instead, use {@link #getCallState} to synchronously query the current call state.
     *
     * @see #EXTRA_PHONE_STATE
     * @see #EXTRA_INCOMING_NUMBER
     * @see #getCallState
     */

    //    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_PHONE_STATE_CHANGED = "chleon.android.intent.action.PHONE_STATE";

    /**
     * The lookup key used with the {@link #ACTION_PHONE_STATE_CHANGED} broadcast
     * for a String containing the new call state.
     *
     * @see #CALL_STATE_IDLE
     * @see #CALL_STATE_OFFHOOK
     * @see #CALL_STATE_RINGING
     * <p class="note">
     * Retrieve with
     * {@link android.content.Intent#getStringExtra(String)}.
     */
    public static final String EXTRA_PHONE_STATE = "android.intent.action.PHONE_STATE";

    /**
     * The lookup key used with the {@link #ACTION_PHONE_STATE_CHANGED} broadcast
     * for a String containing the incoming phone number.
     * Only valid when the new call state is RINGING.
     * <p/>
     * <p class="note">
     * Retrieve with
     * {@link android.content.Intent#getStringExtra(String)}.
     */
    public static final String EXTRA_INCOMING_NUMBER = "incoming_number";

    /**
     * A bond attempt succeeded
     *
     * @hide
     */
    public static final int BOND_SUCCESS = 0;
    /**
     * A bond attempt failed because pins did not match, or remote device did
     * not respond to pin request in time
     *
     * @hide
     */
    public static final int UNBOND_REASON_AUTH_FAILED = 1;
    /**
     * A bond attempt failed because the other side explicitly rejected
     * bonding
     *
     * @hide
     */
    public static final int UNBOND_REASON_AUTH_REJECTED = 2;
    /**
     * A bond attempt failed because we canceled the bonding process
     *
     * @hide
     */
    public static final int UNBOND_REASON_AUTH_CANCELED = 3;
    /**
     * A bond attempt failed because we could not contact the remote device
     *
     * @hide
     */
    public static final int UNBOND_REASON_REMOTE_DEVICE_DOWN = 4;
    /**
     * A bond attempt failed because a discovery is in progress
     *
     * @hide
     */
    public static final int UNBOND_REASON_DISCOVERY_IN_PROGRESS = 5;
    /**
     * A bond attempt failed because of authentication timeout
     *
     * @hide
     */
    public static final int UNBOND_REASON_AUTH_TIMEOUT = 6;
    /**
     * A bond attempt failed because of repeated attempts
     *
     * @hide
     */
    public static final int UNBOND_REASON_REPEATED_ATTEMPTS = 7;
    /**
     * A bond attempt failed because we received an Authentication Cancel
     * by remote end
     *
     * @hide
     */
    public static final int UNBOND_REASON_REMOTE_AUTH_CANCELED = 8;
    /**
     * An existing bond was explicitly revoked
     *
     * @hide
     */
    public static final int UNBOND_REASON_REMOVED = 9;
    /* @hide */
    public static final int UNBOND_REASON_MAX_DEVICE_REACHED = 10;

    /**
     * The user will be prompted to enter a pin
     *
     * @hide
     */
    public static final int PAIRING_VARIANT_PIN = 0;
    /**
     * The user will be prompted to enter a passkey
     *
     * @hide
     */
    public static final int PAIRING_VARIANT_PASSKEY = 1;
    /**
     * The user will be prompted to confirm the passkey displayed on the screen
     *
     * @hide
     */
    public static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;
    /**
     * The user will be prompted to accept or deny the incoming pairing request
     *
     * @hide
     */
    public static final int PAIRING_VARIANT_CONSENT = 3;
    /**
     * The user will be prompted to enter the passkey displayed on remote device
     *
     * @hide
     */
    public static final int PAIRING_VARIANT_DISPLAY_PASSKEY = 4;
    /**
     * The user will be prompted to accept or deny the OOB pairing request
     *
     * @hide
     */
    public static final int PAIRING_VARIANT_OOB_CONSENT = 5;
    /**
     * Used as an extra field in {@link #ACTION_UUID} intents,
     * Contains the {@link android.os.ParcelUuid}s of the remote device which
     * is a parcelable version of {@link java.util.UUID}.
     *
     * @hide
     */
    public static final String EXTRA_UUID = "android.bluetooth.device.extra.UUID";

    /**
     * Flag constant used in {@link #setBondFlags(int)}, to
     * bond the device in Phone Mode. Used for basic
     * communication for phone and contacts to the connected device.
     */
    public static final int FLAG_BOND_PHONE = 0x40;
    /**
     * Flag constant used in {@link #setBondFlags(int)}, to
     * bond the device in Music Mode. Used for basic media
     * communication to the connected device.
     */
    public static final int FLAG_BOND_MUSIC = 0x20;
    /**
     * Flag constant used in {@link #setBondFlags(int)}, to
     * clear the bond type set earlier.
     */
    public static final int FLAG_BOND_NONE = 0x00;

    /**
     * Lazy initialization. Guaranteed final after first object constructed, or
     * getService() called.
     * TODO: Unify implementation of sService amongst BluetoothFoo API's
     */

    /**
     * Broadcast Action: The phone's signal strength has changed. The intent will have the
     * following extra values:</p>
     * <ul>
     * <li><em>EXTRA_SIGNAL_STRENGTH</em> - A numeric value for the signal strength.
     * EXTRA_SIGNAL_STRENGTH is 0-5 or -1 if unknown .
     * </li>
     * </ul>
     * <p/>
     * <p class="note">
     * Requires the {@link android.Manifest.permission#READ_PHONE_STATE} permission.
     * <p/>
     * <p class="note">This is a protected intent that can only be sent
     * by the system.
     */
    public static final String ACTION_SIGNAL_STRENGTH_CHANGED = "chleon.android.intent.action.SIG_STRENGTH";

    /**
     * Value used with {@link #ACTION_SIGNAL_STRENGTH_CHANGED}
     */
    public static final String EXTRA_SIGNAL_STRENGTH = "android.intent.action.SIGNAL_STRENGTH";
    //Constants

    /**
     * Device call state: No activity.
     */
    public static final int CALL_STATE_IDLE = 0;
    /**
     * Device call state: Ringing. A new call arrived and is
     * ringing or waiting. In the latter case, another call is
     * already active.
     */
    public static final int CALL_STATE_RINGING = 1;

    /**
     * Device call state: Off-hook. At least one call exists
     * that is dialing, active, or on hold, and no calls are ringing
     * or waiting.
     */
    public static final int CALL_STATE_OFFHOOK = 2;

    /**
     * Device call state: Dialing.
     */
    public static final int CALL_STATE_DIALING = 3;

    /** Device call state: Hold. */
    //public static final int CALL_STATE_HOLD = 4;


    /** Network type is BT */
    /**
     * Network type is BT
     */
    public static final int NETWORK_TYPE_BT = 0;
    /**
     * Network type is GSM
     */
    public static final int NETWORK_TYPE_GSM = 1;
    /**
     * Current network is GPRS
     */
    public static final int NETWORK_TYPE_GPRS = 2;
    /**
     * Current network is EGPRS
     */
    public static final int NETWORK_TYPE_EGPRS = 3;
    /**
     * Current network is WCDMA
     */
    public static final int NETWORK_TYPE_WCDMA = 4;
    /**
     * Current network is HSDPA
     */
    public static final int NETWORK_TYPE_HSDPA = 5;
    /**
     * Current network is HSUPA
     */
    public static final int NETWORK_TYPE_HSUPA = 6;
    /**
     * Network type is unknown
     */
    public static final int NETWORK_TYPE_UNKNOWN = 7;


    public static final String ACTION_PHONEBOOK_SYNC_STATUS = "chleon.android.bluetooth.device.action.phonebook.status";

    public static final String ACTION_MESSAGES_SYNC_STATUS = "chleon.android.bluetooth.device.action.messages.status";

    public static final String EXTRA_PB_SYNC_STATE = "android.intent.action.PB_STATE";
    public static final String EXTRA_MSG_SYNC_STATE = "android.intent.action.MSG_STATE";


    private static IBluetoothManager sService;

    public static final int REPEATE_CURRENT_SONG = 0x02;
    /**
     * Repeate mode for All song
     */
    public static final int REPEATE_ALL_SONGS = 0x03;

    private final String mAddress;

    /*package*/
    static IBluetoothManager getService()
    {
        synchronized (BluetoothDevice.class)
        {
            if (sService == null)
            {
                sService = BluetoothManager.mService;
            }
        }
        return sService;
    }

    /**
     * Create a new BluetoothDevice
     * Bluetooth MAC address must be upper case, such as "00:11:22:33:AA:BB",
     * and is validated in this constructor.
     *
     * @param address valid Bluetooth MAC address
     * @throws RuntimeException         Bluetooth is not available on this platform
     * @throws IllegalArgumentException address is invalid
     * @hide
     */
    /*package*/ BluetoothDevice(String address)
    {
        getService();  // ensures sService is initialized
        if (!BluetoothManager.checkBluetoothAddress(address))
        {
            throw new IllegalArgumentException(address + " is not a valid Bluetooth address");
        }

        mAddress = address;
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof BluetoothDevice)
        {
            return mAddress.equals(((BluetoothDevice) o).getAddress());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return mAddress.hashCode();
    }

    /**
     * Returns a string representation of this BluetoothDevice.
     * <p>Currently this is the Bluetooth hardware address, for example
     * "00:11:22:AA:BB:CC". However, you should always use {@link #getAddress}
     * if you explicitly require the Bluetooth hardware address in case the
     * {@link #toString} representation changes in the future.
     *
     * @return string representation of this BluetoothDevice
     */
    @Override
    public String toString()
    {
        return mAddress;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    public static final Creator<BluetoothDevice> CREATOR = new Creator<BluetoothDevice>()
    {
        @Override
        public BluetoothDevice createFromParcel(Parcel in)
        {
            return new BluetoothDevice(in.readString());
        }

        @Override
        public BluetoothDevice[] newArray(int size)
        {
            return new BluetoothDevice[size];
        }
    };

    @Override
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeString(mAddress);
    }

    /**
     * Returns the hardware address of this BluetoothDevice.
     * <p> For example, "00:11:22:AA:BB:CC".
     *
     * @return Bluetooth hardware address as string
     */
    public String getAddress()
    {
        return mAddress;
    }

    //chinese_char_len: 3 means UTF-8 code,  sum :means the number of the split
    private String splitChinese(int chinese_char_len, String str, int sum)
    {
        final int charset = chinese_char_len;
        if (charset < 2 || 3 < charset)
        {
            return str;
        }
        int index = sum - 1;
        if (null == str || "".equals(str))
        {
            return str;
        }
        if (index <= 0)
        {
            return str;
        }

        byte[] bt = null;
        try
        {
            if (charset == 2)
            {
                bt = str.getBytes();
            }
            else
            {
                bt = str.getBytes("UTF-8");
            }
        }
        catch (final UnsupportedEncodingException e)
        {
            e.getMessage();
        }
        if (null == bt)
        {
            return str;
        }
        if (index > bt.length - 1)
        {
            index = bt.length - 1;
        }

        if (bt[index] < 0)
        {
            int jsq = 0;
            int num = index;
            while (num >= 0)
            {
                if (bt[num] < 0)
                {
                    jsq += 1;
                }
                else
                {
                    break;
                }
                num -= 1;
            }

            int m = 0;
            if (charset == 2)
            {
                m = jsq % 2;
                index -= m;
                final String substrx = new String(bt, 0, index + 1);
                return substrx;
            }
            else
            {
                m = jsq % 3;
                index -= m;
                String substrx = null;
                try
                {
                    substrx = new String(bt, 0, index + 1, "UTF-8");
                }
                catch (final UnsupportedEncodingException e)
                {
                    e.getMessage();
                }
                return substrx;
            }
        }
        else
        {
            String substrx = null;
            if (charset == 2)
            {
                substrx = new String(bt, 0, index + 1);
                return substrx;
            }
            else
            {
                try
                {
                    substrx = new String(bt, 0, index + 1, "UTF-8");
                }
                catch (final UnsupportedEncodingException e)
                {
                    e.getMessage();
                }
                return substrx;
            }
        }
    }


    /**
     * Get the friendly Bluetooth name of the remote device.
     * <p/>
     * <p>The local adapter will automatically retrieve remote names when
     * performing a device scan, and will cache them. This method just returns
     * the name for this device from the cache.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}
     *
     * @return the Bluetooth name, or null if there was a problem.
     */
    public String getName()
    {
        try
        {
            String name = sService.getRemoteName(mAddress);
            return splitChinese(3, name, 30);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return null;
    }

    /**
     * Start the bonding (pairing) process with the remote device.
     * <p>This is an asynchronous call, it will return immediately. Register
     * for {@link #ACTION_BOND_STATE_CHANGED} intents to be notified when
     * the bonding process completes, and its result.
     * <p>Android system services will handle the necessary user interactions
     * to confirm and complete the bonding process.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}.
     *
     * @return false on immediate error, true if bonding will begin
     * @hide
     */
    public boolean createBond()
    {
        try
        {
            return sService.createBond(mAddress);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Start the connecting process with the remote device only if device is
     * paired earlier.
     * <p>This is an asynchronous call, it will return immediately. Register
     * for {@link #ACTION_STATE_CHANGED} intents to be notified when
     * the bonding process completes, and its result.
     * <p>Android system services will handle the necessary user interactions
     * to confirm and complete the bonding process.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}.
     *
     * @return false on immediate error, true if bonding will begin
     * @hide
     */
    public boolean connect()
    {
        try
        {
            return sService.setDeviceConnected(mAddress, true);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Stops the connecting process with the remote device .
     * <p>This is an asynchronous call, it will return immediately. Register
     * for {@link #ACTION_STATE_CHANGED} intents to be notified when
     * the bonding process completes, and its result.
     * <p>Android system services will handle the necessary user interactions
     * to confirm and complete the bonding process.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}.
     *
     * @return false on immediate error, true if bonding will begin
     * @hide
     */
    public boolean disconnect()
    {
        try
        {
            return sService.setDeviceConnected(mAddress, false);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Get the connected state of the remote device.
     * <p>Possible values for the bond state are:
     * {@link #CONNECT_CONNECTED},
     * {@link #CONNECT_CONNECTING},
     * {@link #CONNECT_DISCONNECTED} or
     * {@link #CONNECT_DISCONNECTING}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}.
     *
     * @return the Connected state
     */
    public int getState()
    {
        try
        {
            return sService.getState(mAddress);
        }
        catch (RemoteException ex)
        {

        }
        return 0;
    }

    /**
     * Cancel an in-progress bonding request started with {@link #createBond}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}.
     *
     * @return true on success, false on error
     * @hide
     */
    public boolean cancelBondProcess()
    {
        try
        {
            return sService.cancelBondProcess(mAddress);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Remove bond (pairing) with the remote device.
     * <p>Delete the link key associated with the remote device, and
     * immediately terminate connections to that device that require
     * authentication and encryption.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN}.
     *
     * @return true on success, false on error
     * @hide
     */
    public boolean removeBond()
    {
        try
        {
            return sService.removeBond(mAddress);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Get the bond state of the remote device.
     * <p>Possible values for the bond state are:
     * {@link #BOND_NONE},
     * {@link #BOND_BONDING},
     * {@link #BOND_BONDED}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH}.
     *
     * @return the bond state
     */
    public int getBondState()
    {
        try
        {
            return sService.getBondState(mAddress);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return BOND_NONE;
    }


    /**
     * @hide
     */
    public ParcelUuid[] getUuids()
    {
        try
        {
            return sService.getRemoteUuids(mAddress);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return null;
    }


    /**
     * @hide
     */
    public boolean setPin(byte[] pin)
    {
        try
        {
            return sService.setPin(mAddress, pin);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }


    /**
     * @hide
     */
    public boolean setPairingConfirmation(boolean confirm)
    {
        try
        {
            return sService.setPairingConfirmation(mAddress, confirm);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }


    /**
     * @hide
     */
    public boolean cancelPairingUserInput()
    {
        try
        {
            return sService.cancelPairingUserInput(mAddress);
        }
        catch (RemoteException e)
        {
            WLog.e(TAG, "", e);
        }
        return false;
    }

    /**
     * Returns a constant indicating the call state (cellular) on the device.
     */
    public int getCallState()
    {
        try
        {
            return sService.getCallState();
        }
        catch (RemoteException ex)
        {
        }
        return CALL_STATE_IDLE;
    }


    /**
     *
     */
    public String getCallDetail()
    {
        try
        {
            return sService.getCallDetail();
        }
        catch (RemoteException ex)
        {
        }
        return null;
    }


    /**
     * Returns a constant indicating the radio technology (network type)
     * currently in use on the device.
     *
     * @return the network type
     * @see #NETWORK_TYPE_BT
     * @see #NETWORK_TYPE_GSM
     * @see #NETWORK_TYPE_GPRS
     * @see #NETWORK_TYPE_EGPRS
     * @see #NETWORK_TYPE_WCDMA
     * @see #NETWORK_TYPE_HSUPA
     * @see #NETWORK_TYPE_UNKNOWN
     */
    public int getNetworkType()
    {
        try
        {
            return sService.getNetworkType();

        }
        catch (RemoteException ex)
        {
        }
        return NETWORK_TYPE_UNKNOWN;
    }


    public int getSignalStrength()
    {
        try
        {
            return sService.getSignalStrength();
        }
        catch (RemoteException ex)
        {
        }
        return -1;
    }

    /**
     * Check that a pin is valid and convert to byte array.
     * <p/>
     * Bluetooth pin's are 1 to 16 bytes of UTF-8 characters.
     *
     * @param pin pin as java String
     * @return the pin code as a UTF-8 byte array, or null if it is an invalid
     * Bluetooth pin.
     * @hide
     */
    public static byte[] convertPinToBytes(String pin)
    {
        if (pin == null)
        {
            return null;
        }
        byte[] pinBytes;
        try
        {
            pinBytes = pin.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException uee)
        {
            WLog.e(TAG, "UTF-8 not supported?!?");  // this should not happen
            return null;
        }
        if (pinBytes.length <= 0 || pinBytes.length > 16)
        {
            return null;
        }
        return pinBytes;
    }

    /**
     * Sets the bonding type of the remote device.
     * It must be called before calling {@link #createBond()}
     *
     * @param bondFlag Constant one or more from
     *                 {@link #FLAG_BOND_MUSIC}, {@link #FLAG_BOND_PHONE} or
     *                 {@link #FLAG_BOND_NONE}. It can be used single or
     *                 like <code>{@link #FLAG_BOND_MUSIC} | {@link #FLAG_BOND_PHONE}</code>
     * @return true is bond flag set.
     */
    public boolean setBondFlags(int bondFlag)
    {
        return false;
    }

    /**
     * Returns the bonding type of the remote device.
     * This also represents the feature supported by device, ie.
     * if device supports media returned flag will contain
     * {@link #FLAG_BOND_MUSIC}, similarly, if device can has
     * phone support, returned value will contain {@link #FLAG_BOND_PHONE}.
     *
     * @return bondFlag Constant one or more from
     * {@link #FLAG_BOND_MUSIC}, {@link #FLAG_BOND_PHONE} or
     * {@link #FLAG_BOND_NONE}. It can be used single or
     * like <code>{@link #FLAG_BOND_MUSIC} | {@link #FLAG_BOND_PHONE}</code>
     */
    public int getBondFlags()
    {
        return 0;
    }

    /**
     * Starts or resumes playback. If playback had previously been paused,
     * playback will continue from where it was paused. If playback had
     * been stopped, or never started before, playback will start at the
     * beginning.
     *
     * @throws IllegalStateException if it is called on an Unsupported device.
     * @see #getBondFlags()
     * @see #isPlaying()
     */
    public void resume() throws IllegalStateException
    {
        try
        {
            sService.musicResume();
        }
        catch (RemoteException ex)
        {
        }
    }

    /**
     * Stops playback after playback has been stopped or paused.
     *
     * @throws IllegalStateException if it is called on an Unsupported device.
     * @see #getBondFlags()
     * @see #isPlaying()
     */
    public void stop() throws IllegalStateException
    {
        try
        {
            sService.musicStop();

        }
        catch (RemoteException ex)
        {
        }
    }

    /**
     * Pauses playback. Call start() to resume.
     *
     * @throws IllegalStateException if it is called on an Unsupported device.
     * @see #getBondFlags()
     * @see #isPlaying()
     */
    public void pause() throws IllegalStateException
    {
        try
        {
            sService.musicPause();

        }
        catch (RemoteException ex)
        {
        }
    }

    /**
     * Play next track , If play list has reached at its tail it will play
     * the first song of the play list.
     *
     * @throws IllegalStateException if it is called on an Unsupported device.
     * @see #getBondFlags()
     * @see #isPlaying()
     */
    public void playNextTrack() throws IllegalStateException
    {
        try
        {
            sService.playNextTrack();

        }
        catch (RemoteException ex)
        {
        }
    }

    /**
     * Plays previous track. If play list has reached at its head it will play
     * the last song of the play list.
     *
     * @throws IllegalStateException if it is called on an Unsupported device.
     * @see #getBondFlags()
     * @see #isPlaying()
     */
    public void playPreviousTrack() throws IllegalStateException
    {
        try
        {
            sService.playPreviousTrack();

        }
        catch (RemoteException ex)
        {
        }
    }

    /**
     * Gets the current playback position.
     *
     * @return the current position in milliseconds
     */
    public int getCurrentPosition()
    {
        try
        {
            return sService.getCurrentPosition();

        }
        catch (RemoteException ex)
        {
        }
        return 0;
    }

    /**
     * Gets the duration of the file.
     *
     * @return the duration in milliseconds
     */
    public int getDuration()
    {
        try
        {
            return sService.getDuration();

        }
        catch (RemoteException ex)
        {
        }
        return 0;
    }

    /**
     * Checks whether the MediaPlayer is playing.
     *
     * @return true if currently playing, false otherwise
     */
    public boolean isPlaying()
    {
        try
        {
            return sService.isPlaying();

        }
        catch (RemoteException ex)
        {
        }
        return false;
    }

    /**
     * gets the artist of the file
     *
     * @return String if artist of the file exist , null orther wise
     */

    public String getArtist() throws IllegalStateException
    {
        try
        {
            return sService.getArtist();

        }
        catch (RemoteException ex)
        {
        }
        return null;
    }

    /**
     * gets the album of the file
     *
     * @return String if album of the file exist , null orther wise
     */
    public String getAlbum() throws IllegalStateException
    {
        try
        {
            return sService.getAlbum();

        }
        catch (RemoteException ex)
        {
        }
        return null;
    }

    /**
     * gets the gener of the file
     *
     * @return String if genre of the file exist , null orther wise
     */
    public String getGenre() throws IllegalStateException
    {
        try
        {
            return sService.getGenre();

        }
        catch (RemoteException ex)
        {
        }
        return null;
    }

    /**
     * gets the composer of the file
     *
     * @return String if composer of the file exist , null orther wise
     */
    public String getComposer() throws IllegalStateException
    {
        try
        {
            return sService.getComposer();

        }
        catch (RemoteException ex)
        {
        }
        return null;
    }

    /**
     * gets the track of the file
     *
     * @return int if track of the file exist , null otherwise
     */

    public int getTrack() throws IllegalStateException
    {
        try
        {
            return sService.getTrack();

        }
        catch (RemoteException ex)
        {
        }
        return 0;
    }

    /**
     * gets the Tittle of the file
     *
     * @return String if Tittle of the file exist , null otherwise
     */

    public String getTittle() throws IllegalStateException
    {
        try
        {
            return sService.getTittle();

        }
        catch (RemoteException ex)
        {
        }
        return null;
    }

    /**
     * sets the repeate mode of the file where mode can be
     * {@link #REPEATE_CURRENT_SONG},{@link #REPEATE_ALL_SONG}
     *
     * @return boolean status of setting repeate mode
     */
    public boolean setRepeateMode(int mode)
    {
        try
        {
            return sService.setRepeateMode(mode);

        }
        catch (RemoteException ex)
        {
        }
        return false;
    }

    /**
     * gets the repeate mode of the file where mode can be {@link REPEATE_CURRENT_SONG},{@link REPEATE_ALL_SONG}
     *
     * @return int status of setting repeate mode
     */
    int getRepeateMode()
    {
        try
        {
            return sService.getRepeateMode();
        }
        catch (RemoteException ex)
        {
        }
        return 0;
    }

    /**
     * mutes the current running song
     * This is asynchronus procedure.
     */
    public boolean setMuteState(boolean state)
    {
        try
        {
            return sService.setMuteState(state);
        }
        catch (RemoteException ex)
        {

        }
        return false;
    }

    /**
     * Place a call to the specified number.
     * This is asynchronus procedure.
     *
     * @param number the number to be called.
     */
    public void call(String number)
    {
        try
        {
            sService.call(number);

        }
        catch (RemoteException ex)
        {
        }
    }

    /**
     * Place a call from recent call list
     * to connected phone .
     * This is asynchronus procedure.
     */
    public void callLastNumber()
    {
        try
        {
            sService.callLastNumber();
        }
        catch (RemoteException ex)
        {
        }
    }

    /**
     * Receive an incoming call
     * This is asynchronus procedure.
     */
    public void recieveIncomingCall()
    {
        try
        {
            sService.recieveIncomingCall();
        }
        catch (RemoteException ex)
        {
        }
    }


    /**
     * disconnect a call
     * This is asynchronus procedure.
     */
    public void disconnectCall()
    {
        try
        {
            sService.disconnectCall();
        }
        catch (RemoteException ex)
        {
        }
    }

    /**
     * reject an Incomming call
     * This is asynchronus procedure.
     */
    public boolean rejectCall()
    {
        try
        {
            return sService.rejectCall();
        }
        catch (RemoteException ex)
        {
        }
        return false;
    }


    /**
     * muteCall the current running Call
     * This is asynchronus procedure.
     */

    public boolean muteCall(boolean state)
    {
        try
        {
            return sService.muteCall(state);
        }
        catch (RemoteException e)
        {
        }
        return false;
    }

    public boolean isSupportPhoneBook()
    {
        try
        {
            return sService.isSupportPhoneBook(mAddress);
        }
        catch (RemoteException e)
        {
        }
        return false;
    }

    public boolean isPhoneBookSynced()
    {
        try
        {
            return sService.isPhoneBookSynced(mAddress);
        }
        catch (RemoteException e)
        {
        }
        return false;
    }

    public int getPhoneBookSyncedStatus()
    {
        try
        {
            return sService.getPhoneBookSyncedStatus(mAddress);
        }
        catch (RemoteException e)
        {
        }
        return -2;
    }


    public void getPhoneBookByManual()
    {
        try
        {
            sService.getPhoneBookByManual(mAddress);
        }
        catch (RemoteException e)
        {
        }
    }


    public int getPhoneBookSyncProgress()
    {
        try
        {
            return sService.getPhoneBookSyncProgress(mAddress);
        }
        catch (RemoteException e)
        {
        }
        return 0;
    }

    public boolean startPlayer()
    {
        try
        {
            return sService.playerStart(mAddress);
        }
        catch (RemoteException e)
        {
        }
        return false;
    }


}

