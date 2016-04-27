package com.service.bluetooth;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TreeMap;
import java.util.regex.Pattern;


public class BtManagerService extends IBluetoothManager.Stub
{
    public static final boolean DBG = true;
    public static final String TAG = "BtManager";
    private static final String DEVICE_XF6000_PATH = "/sys/devices/platform/imx-i2c.2/i2c-2/2-0047/work_mode";
    private final ArrayList<Record> mRecords = new ArrayList();

    private static final String ACTION_SYS_GOTO_SLEEP = "vehicle://9001";

    private IBluetoothPbapCallback pbapCallback = null;

    Context mContext;
    MainThreadHandler mMainThreadHandler;
    BtCommandInterface commandInterface;
    MediaInfo mMediaInfo;
    BtHost mBtHost;
    HandlerThread mBtMainThread, mMusicThread;
    MusicHandler mMusicHandler;
    boolean isOnBTSource = false;
    boolean isListeningBTSource = false;

    static final int DEFAULT_WAKE_LOCK_TIMEOUT = 5000;
    static final long MAX_TIMEOUT_FOR_API = 7000;
    static int max_pair_count = -1;
    static final long MAX_DISCOVERY_TIME_IN_MILLIS = 30 * 1000; // 30 sec

    final Object mBtDeviceLock = new Object();
    //	TreeMap<String, BtDevice> mAddrDeviceMap;
    TreeMap<String, BtDeviceRemote> mRemoteDeviceMap;
    protected int mPairingMode;
    // private final UserInputSource mUserSource;
    // IPeripheralManager iPeripheral;
    //	PeripheralManagerService iPeripheral;
    //boolean mMediaInfoUpdatePending = false;

    String mBTVersion = null;

    ArrayList<CallStatus> mCallStatusArray = new ArrayList<CallStatus>();

    public HandlerThread sWorkerThread;
    public Handler sWorker;

    public HandlerThread sWorkerIncallCheckThread;
    public Handler sWorkerIncallCheck;


    public HandlerThread sBTMusicPlayerThread;
    public Handler sBTMusicPlayer;

    public HandlerThread sBTCheckSysPowerThread;
    public Handler sBTCheckSysPower;

    public HandlerThread sBTCheckConnectThread;
    public Handler sBTCheckConnect;

    public HandlerThread sBTCheckEnableStatusThread;
    public Handler sBTCheckEnableStatus;

    private boolean isUserConfirmed = false;

    private TextToSpeech mTts;

    boolean mAllowConnect = false;

    String mLastMusicTile = null;

    final Object mBTMusicSourceLock = new Object();
    final Object mPhoneModeLock = new Object();
    final Object mAppModeLock = new Object();

    private LocationManager mLocationManager;
    private int mTimeUpdateSpacing = 1000 * 60 * 10;
    private int mTimeUpdateDiff = 2000;
    private long mSavedAtTime = 0;

    private Boolean mHasAudioFocus = false;
    // Message codes used with mMainThreadHandler
    static final int CMD_ENABLE = 2;
    static final int CMD_DISABLE = 3;
    static final int CMD_GET_NAME = 4;
    static final int CMD_SET_NAME = 5;
    static final int CMD_START_DISCOVERABLE = 6;
    static final int CMD_STOP_DISCOVERABLE = 7;
    static final int CMD_START_INQUIRY = 8;
    static final int CMD_STOP_INQUIRY = 9;
    static final int CMD_GET_BONDED_DEVICE_COUNT = 10;
    static final int CMD_INITIATE_PAIRING = 11;
    static final int CMD_ACCPET_INCOMING_PASSKEY_PAIRING = 12;
    static final int CMD_SET_PAIRING_MODE = 13;
    static final int CMD_DELETE_TRUSTED = 14;
    static final int CMD_UNTRUSTED_LIST = 15;
    static final int CMD_TRUSTED_LIST = 16;
    static final int CMD_INITIATE_CALL = 17;
    static final int CMD_INITIATE_LAST_CALL = 18;
    static final int CMD_RECIEVE_INCOMING_CALL = 19;
    static final int CMD_PLAYER_ACTION = 20;
    static final int CMD_PLAYER_STATUS = 21;
    static final int CMD_PLAYER_MUTE_STATE = 22;
    static final int CMD_MEDIA_META_DATA = 23;
    static final int CMD_PALYER_SET_REPEATE_MODE = 24;
    static final int CMD_CALL_STATE = 25;
    static final int CMD_CALL_HANG_UP = 26;
    static final int CMD_DEVICE_CONNECT_DISCONNECT = 27;
    static final int CMD_PALYER_GET_REPEATE_MODE = 28;
    static final int CMD_SET_VOLUME = 29;
    static final int CMD_GET_DISCRET_MODE = 34;
    static final int CMD_SET_DISCRET_MODE = 35;
    static final int CMD_GET_MIC_MUTE_STATE = 36;
    static final int CMD_SET_MIC_MUTE_STATE = 37;
    static final int CMD_GENERATE_DTMF = 38;
    static final int CMD_SWITCH_CALLS = 41;
    static final int CMD_GET_VOLUME = 42;
    static final int CMD_GET_VOLUME_RANGE = 43;
    static final int CMD_GET_PB_SYNC_MANUAL = 44;
    static final int CMD_GET_PB_MULTI_SYNC = 45;
    static final int CMD_GET_AUTOCONN_MODE = 46;
    static final int CMD_SET_AUTOCONN_MODE = 47;
    static final int CMD_SET_AADC = 48;
    static final int CMD_GET_SW_VERSION = 49;
    static final int CMD_ENABLE_UPDATE = 50;
    static final int CMD_GET_PLAYER_ID = 51;
    static final int CMD_GET_RSSI = 52;

    ////////////////////////////////////////////////
    static final int CMD_GET_REMOTE_DEVICE_NAME = 61;
    static final int CMD_SET_PROFILE_MSK = 62;
    static final int CMD_GET_PHONE_BOOK = 63;
    static final int CMD_GET_MSG_LIST = 64;
    static final int CMD_GET_MSG_LIST_CONT = 65;
    static final int CMD_GET_MSG_LIST_CMT = 66;
    static final int CMD_GET_MSG = 67;
    static final int CMD_SEND_MSG = 68;
    static final int CMD_FINISH_PULL_PHONEBOOK = 69;
    static final int CMD_GET_CALL_INFO = 70;
    static final int CMD_MULTICALL_ACTION = 71;

    ////////////////////////////////////////////////

    static final int CMD_DONE_BOOLEAN = 100;
    static final int CMD_DONE_GET_NAME = 104;
    static final int CMD_DONE_GET_BONDED_DEVICE_COUNT = 105;
    static final int CMD_DONE_UNTRUSTED_LIST = 106;
    //	static final int CMD_DONE_TRUSTED_LIST = 107;
    static final int CMD_DONE_PLAYER_STATUS = 108;
    static final int CMD_DONE_MEDIA_META_DATA = 109;
    static final int CMD_DONE_ENABLE = 110;
    static final int CMD_DONE_DISABLE = 111;
    static final int CMD_DONE_CALL_STATE = 112;
    static final int CMD_DONE_DEVICE_CONNECT_DISCONNECT = 113;
    static final int CMD_DONE_START_INQUIRY = 114;
    static final int CMD_DONE_PALYER_GET_REPEATE_MODE = 115;
    static final int CMD_DONE_GET_DISCRET_MODE = 134;
    static final int CMD_DONE_GET_MIC_MUTE_STATE = 136;
    static final int CMD_DONE_GET_AUDIO_VOLUME = 137;
    static final int CMD_DONE_GET_AUDIO_RANGE = 138;
    static final int CMD_DONE_GET_AUTOCONN_MODE = 139;
    static final int CMD_DONE_GET_SW_VERSION = 140;
    static final int CMD_DONE_GET_PLAYER_ID = 141;
    static final int CMD_DONE_HANGUP_CALL = 142;
    static final int CMD_DONE_GET_RSSI = 143;

    ////////////////////////////////////////////////
    static final int CMD_DONE_GET_REMOTE_DEVICE_NAME = 200;
    ////////////////////////////////////////////////

    static final int EVENT_DEVICE_INQUIRY = 1000;
    static final int EVENT_DEVICE_INQUIRY_FINISHED = 1001;
    static final int EVENT_DEVICE_PAIRING_RESPONSE = 1002;
    static final int EVENT_DEVICE_PASSKEY_RESPONSE = 1003;
    static final int EVENT_DEVICE_REMEOVE_BONDED = 1004;
    static final int EVENT_DEVICE_PLAYER_METADATA = 1005;
    static final int EVENT_DEVICE_CALL_STATUS = 1006;
    //	static final int EVENT_DEVICE_CONNECT_STATE_CHANGED = 1007;
    static final int EVENT_DEVICE_PALYER_STATUS = 1008;
    static final int EVENT_BT_ENABLE_STATE_CHANGED = 1009;
    static final int EVENT_BT_POWER_READY = 1010;
    static final int EVENT_BT_PLAYER_SOURCE_STATUS = 1011;
    static final int EVENT_BT_HOST_ADDRES = 1012;

    ////////////////////////////////////////////////
    static final int EVENT_BT_HFP_CONNECT_STATE_CHANGED = 2000;
    static final int EVENT_BT_HFP_SERVICE_STATE_CHANGED = 2001;
    static final int EVENT_BT_A2DP_CONNECT_STATE_CHANGED = 2002;
    static final int EVENT_BT_A2DP_SERVICE_STATE_CHANGED = 2003;
    static final int EVENT_BT_PBAP_CONNECT_STATE_CHANGED = 2004;
    static final int EVENT_BT_PBAP_SERVICE_STATE_CHANGED = 2005;
    static final int EVENT_BT_MAP_CONNECT_STATE_CHANGED = 2006;

    static final int EVENT_BT_REMOTE_NAME_CHANGED = 2007;
    static final int EVENT_BT_PULLPB_CMT_CHANGED = 2008;
    static final int EVENT_BT_GETMSG_IND_CHANGED = 2009;
    static final int EVENT_BT_GETMSG_CMT_CHANGED = 2010;
    static final int EVENT_BT_PUSHMSG_IND_CHANGED = 2011;
    static final int EVENT_BT_PUSHMSG_CMT_CHANGED = 2012;
    static final int EVENT_BT_MSGEVT_STAUS_CHANGED = 2013;
    static final int EVENT_BT_HFAUDIO_STAUS_CHANGED = 2014;
    static final int EVENT_BT_LOCAL_NAME_CHANGED = 2015;
    static final int EVENT_BT_MUTE_STATUS_CHANGED = 2016;
    static final int EVENT_BT_PAIRED_LIST = 2017;

    static final int EVENT_BT_AVRCP_CONNECT_STATE_CHANGED = 2018;
    static final int EVENT_BT_PHONEBOOK_SIZE_CHANGED = 2019;

    static final int EVENT_BT_VOLUME_GAIN_CHANGED = 2020;

    static final int EVENT_BT_RING_STATUS_CHANGED = 2021;

    static final int EVENT_BT_AVRCP_PLAYCMD_STATE_CHANGED = 2022;

    static final int EVENT_BT_AVRCP_PAUSECMD_STATE_CHANGED = 2023;

    ////////////////////////////////////////////////

    public static final String BT_MUSIC_ACTION = "com.chleon.bluetooth.intent.ACTION_PLAY_MUSIC";

    String mBtHostName = null;

    boolean isBTStandby = false;

    final static int PLAYER_IDLE = 0;
    final static int PLAYER_STOPING = 1;

    int mPlayerStoping = PLAYER_IDLE;

    int mMicUnMuteState = 1;
    int mPhoneHFAudioConnected = 0;

    //////////////////////////////////////////////////////
    String mCurrenAddr = null;
    boolean mIsInCallState = false;
    CallStatus mCurrentCall = null;
    String mLastConnectedAddr = null;
    long mHFPConnectionTimeStamp = 0;

    int mCurrentCallState = BluetoothDevice.CALL_STATE_IDLE;
    int mLastCallState = BluetoothDevice.CALL_STATE_IDLE;
    int mLastCallSz = 0;
    int mCurrentCallSz = 0;
    boolean mIncallBeforeConnection = false;

    int mHFPState = -1;
    int mA2DPState = -1;
    int mPBAPState = -1;

    Thread mAutoConnectThread;
    AutoConnectedRunnable mAutoConnRunnabler;

    int mPhoneVol = 7;


    private static class Record
    {
        String pkgForDebug;

        IBinder binder;

        IBluetoothListener callback;

        int events;
    }

    //////////////////////////////////////////////////////
    static final long RING_TIMEOUT = 12 * 1000 - 200;

    Timer ring_timer = null;

    /**
     * A request object for use with {@link com.service.bluetooth.BtManagerService.MainThreadHandler}. Requesters
     * should wait() on the request after sending. The main thread will notify
     * the request when it is complete.
     */
    private static final class MainThreadRequest
    {
        /**
         * The argument to use for the request
         */
        public Object argument;
        /**
         * The result of the request that is run on the main thread
         */
        public Object result;

        public MainThreadRequest(Object argument)
        {
            this.argument = argument;
        }
    }

    /**
     * A handler that processes messages on the main thread in the phone
     * process. Since many of the Phone calls are not thread safe this is needed
     * to shuttle the requests from the inbound binder threads to the main
     * thread in the phone process. The Binder thread may provide a
     * {@link com.service.bluetooth.BtManagerService.MainThreadRequest} object in the msg.obj field that they are
     * waiting on, which will be notified when the operation completes and will
     * contain the result of the request.
     * <p/>
     * <p/>
     * If a MainThreadRequest object is provided in the msg.obj field, note that
     * request.result must be set to something non-null for the calling thread
     * to unblock.
     */
    private final class MainThreadHandler extends Handler
    {
        public MainThreadHandler(Looper loop)
        {
            super(loop);
        }

        @Override
        public void handleMessage(Message msg)
        {
            AsyncResult ar;
            MainThreadRequest request;
            Message onCompleted;

            try
            {
                switch (msg.what)
                {
                    case CMD_ENABLE:
                    {
                        WLog.d(TAG, "CMD_ENABLE Enter");
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_ENABLE, request);
                        commandInterface.enable(onCompleted);
                        WLog.d(TAG, "CMD_ENABLE Exit");
                        break;
                    }
                    case CMD_DISABLE:
                    {
                        WLog.d(TAG, "CMD_DISABLE Enter");
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_DISABLE, request);
                        commandInterface.disable(onCompleted);
                        //WLog.d(TAG,"JonPHONE:  CMD_DISABLE Exit");
                        break;
                    }
                    case CMD_GET_NAME:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.getName(onCompleted);
                        break;
                    }
                    case CMD_SET_NAME:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setName((String) request.argument, onCompleted);
                        break;
                    }
                    case CMD_START_DISCOVERABLE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        synchronized (mBtHost)
                        {
                            mBtHost.scanMode = BluetoothManager.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
                            Settings.System
                                    .putInt(mContext.getContentResolver(), Settings.System.BLUETOOTH_DISCOVERABILITY,
                                            BluetoothManager.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
                        }

                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setDiscoverable(true, onCompleted);
                        break;
                    }
                    case CMD_STOP_DISCOVERABLE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        synchronized (mBtHost)
                        {
                            mBtHost.scanMode = BluetoothManager.SCAN_MODE_NONE;
                            Settings.System
                                    .putInt(mContext.getContentResolver(), Settings.System.BLUETOOTH_DISCOVERABILITY,
                                            BluetoothManager.SCAN_MODE_NONE);
                        }
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setDiscoverable(false, onCompleted);
                        break;
                    }
                    case CMD_START_INQUIRY:
                    {
                        if (DBG)
                        {
                            WLog.d(TAG, "CMD_START_INQUIRY Enter");
                        }
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_START_INQUIRY, request);
                        commandInterface.setInquiry(true, onCompleted);
                        break;
                    }
                    case CMD_STOP_INQUIRY:
                    {
                        setInquiryMode(false);
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setInquiry(false, onCompleted);
                        break;
                    }
                    case CMD_INITIATE_PAIRING:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface
                                .setBond((String) request.argument, BTConstants.BT_PAIRING_INITIATE, null, onCompleted);
                        break;
                    }
                    case CMD_ACCPET_INCOMING_PASSKEY_PAIRING:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        Object[] data = (Object[]) request.argument;
                        String device = (String) data[0];
                        String passkey = (String) data[1];
                        commandInterface.setBond(device, BTConstants.BT_PAIRING_ACCEPT, passkey, onCompleted);
                        break;
                    }
                    case CMD_SET_PAIRING_MODE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        Object[] dataSetpairing = (Object[]) request.argument;
                        int pairingMode = (Integer) dataSetpairing[0];
                        String pinCode = dataSetpairing[1] == null ? null : (String) dataSetpairing[1];
                        commandInterface.setPairingMode(pairingMode, pinCode, onCompleted);
                        break;
                    }
                    case CMD_UNTRUSTED_LIST:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_UNTRUSTED_LIST, request);
                        commandInterface.requestUntrustedList(onCompleted);
                        break;
                    }
                    case CMD_TRUSTED_LIST:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.requestTrustedList(onCompleted);
                        break;
                    }
                    case CMD_DELETE_TRUSTED:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.deletePairedDevices((String) request.argument, onCompleted);
                        break;
                    }
                    case CMD_PLAYER_ACTION:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.playerAction((Integer) request.argument, onCompleted);
                        break;
                    }
                    case CMD_INITIATE_CALL:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.call((String) request.argument, onCompleted);
                        break;
                    }
                    case CMD_INITIATE_LAST_CALL:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.callLastNumber(null);
                        break;
                    }
                    case CMD_RECIEVE_INCOMING_CALL:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.receiveIncomingCall(null);
                        break;
                    }
                    case CMD_PLAYER_STATUS:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_PLAYER_STATUS, request);
                        commandInterface.playerStatus(onCompleted);
                        break;
                    }
                    case CMD_PLAYER_MUTE_STATE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setMuteState((Boolean) request.argument, onCompleted);
                        break;
                    }
                    case CMD_MEDIA_META_DATA:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_MEDIA_META_DATA, request);
                        commandInterface.getMediaMetaData((Integer) request.argument, onCompleted);
                        break;
                    }
                    case CMD_PALYER_SET_REPEATE_MODE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setRepeatMode((Integer) request.argument, onCompleted);
                        break;
                    }
                    case CMD_CALL_STATE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_CALL_STATE, request);
                        commandInterface.getCallState(onCompleted);
                        break;
                    }
                    case CMD_CALL_HANG_UP:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_HANGUP_CALL, request);
                        Object[] count = (Object[]) request.argument;
                        int callIndex = (Integer) count[0];
                        int callCount = (Integer) count[1];
                        commandInterface.hangUpCall(callIndex, callCount, onCompleted);
                        break;
                    }
                    case CMD_DEVICE_CONNECT_DISCONNECT:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_DEVICE_CONNECT_DISCONNECT, request);
                        Object[] data = (Object[]) request.argument;
                        String address = (String) data[0];
                        int profile = (Integer) data[1];
                        boolean state = (Boolean) data[2];
                        if (state)
                        {
                            mCurrenAddr = address;
                        }
                        commandInterface.setDeviceConnected(address, profile, state, onCompleted);
                        break;
                    }
                    case CMD_PALYER_GET_REPEATE_MODE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_PALYER_GET_REPEATE_MODE, request);
                        commandInterface.getRepeatMode(onCompleted);
                        break;
                    }
                    case CMD_SET_VOLUME:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        int[] btVolume = (int[]) request.argument;
                        int type = btVolume[0];
                        int volume = btVolume[1];
                        commandInterface.setAudioVolume(type, volume, onCompleted);
                        break;
                    }
                    case CMD_GET_DISCRET_MODE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_GET_DISCRET_MODE, request);
                        commandInterface.getPhonePrivateState(onCompleted);
                        break;
                    }
                    case CMD_SET_DISCRET_MODE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setPhonePrivateState((Integer) request.argument, onCompleted);
                        break;
                    }
                    case CMD_GET_MIC_MUTE_STATE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_GET_MIC_MUTE_STATE, request);
                        commandInterface.getMicMuteState(onCompleted);
                        break;
                    }
                    case CMD_SET_MIC_MUTE_STATE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setMicMuteState((Integer) request.argument, onCompleted);
                        break;
                    }
                    case CMD_GENERATE_DTMF:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.generateDTMF((Character) request.argument, onCompleted);
                        break;
                    }
                    case CMD_SWITCH_CALLS:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.switchCalls(onCompleted);
                        break;
                    }
                    case CMD_GET_VOLUME:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_GET_AUDIO_VOLUME, request);
                        commandInterface.getAudioVolume((Integer) request.argument, onCompleted);
                        break;
                    }
                    case CMD_GET_VOLUME_RANGE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_GET_AUDIO_RANGE, request);
                        commandInterface.getAudioVolumeRange((Integer) request.argument, onCompleted);
                        break;
                    }
                    case CMD_GET_PB_SYNC_MANUAL:
                    {
                        //				request = (MainThreadRequest) msg.obj;
                        //				onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        //				BtDevice pbSyncdevice = (BtDevice) request.argument;
                        //				commandInterface
                        //						.setStartPBSyncManual(pbSyncdevice, onCompleted);
                        break;
                    }
                    case CMD_GET_PB_MULTI_SYNC:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setEnableMultiSync((Integer) request.argument, onCompleted);
                        break;
                    }
                    case CMD_GET_AUTOCONN_MODE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_GET_DISCRET_MODE, request);
                        commandInterface.getAutoConnMode(onCompleted);
                        break;
                    }
                    case CMD_SET_AUTOCONN_MODE:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setAutoConnMode((Integer) request.argument, onCompleted);
                        break;
                    }
                    case CMD_SET_AADC:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        int[] config = (int[]) request.argument;
                        int type = config[0];
                        int value = config[1];
                        commandInterface.setADCConfiguration(type, value, onCompleted);
                        break;
                    }
                    case CMD_GET_SW_VERSION:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_GET_SW_VERSION, request);
                        commandInterface.getVersion(onCompleted);
                        break;
                    }
                    case CMD_ENABLE_UPDATE:
                    {
                        break;
                    }
                    case CMD_GET_RSSI:
                    {
                        //				request = (MainThreadRequest) msg.obj;
                        //				onCompleted = obtainMessage(CMD_DONE_GET_RSSI, request);
                        //				BtDevice btDevice = (BtDevice) request.argument;
                        //				commandInterface.btPBRSSI(btDevice, onCompleted);
                        break;
                    }
                    case CMD_GET_REMOTE_DEVICE_NAME:
                    {
                        request = (MainThreadRequest) msg.obj;
                        String address = (String) request.argument;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.getRemoteName(address, onCompleted);
                        break;
                    }
                    case CMD_SET_PROFILE_MSK:
                    {
                        request = (MainThreadRequest) msg.obj;
                        int mask = (Integer) request.argument;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.setProfileSupported(mask, onCompleted);
                        break;
                    }
                    case CMD_GET_PHONE_BOOK:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        Object[] data = (Object[]) request.argument;
                        int storage = (Integer) data[0];
                        int type = (Integer) data[1];
                        int maxlist = (Integer) data[2];
                        int offset = (Integer) data[3];
                        commandInterface.pullPhoneBook(storage, type, maxlist, offset, onCompleted);
                        break;
                    }
                    case CMD_GET_MSG_LIST:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        Object[] data = (Object[]) request.argument;
                        int fold = (Integer) data[0];
                        int maxlist = (Integer) data[1];
                        int offset = (Integer) data[2];
                        commandInterface.getMessageList(fold, maxlist, offset, onCompleted);
                        break;

                    }
                    case CMD_GET_MSG_LIST_CONT:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.getMessageListCont(onCompleted);
                        break;
                    }
                    case CMD_GET_MSG_LIST_CMT:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.getMessageListCmt(onCompleted);
                        break;
                    }
                    case CMD_GET_MSG:
                    {
                        request = (MainThreadRequest) msg.obj;
                        String handler = (String) request.argument;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.getMessage(handler, onCompleted);
                        break;
                    }
                    case CMD_SEND_MSG:
                    {
                        request = (MainThreadRequest) msg.obj;
                        Bundle b = (Bundle) request.argument;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.sendMessage(b, onCompleted);
                        break;
                    }
                    case CMD_FINISH_PULL_PHONEBOOK:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.finishPullPhoneBook(onCompleted);
                        break;
                    }
                    case CMD_GET_CALL_INFO:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.getCallInformation(onCompleted);
                        break;
                    }
                    case CMD_MULTICALL_ACTION:
                    {
                        request = (MainThreadRequest) msg.obj;
                        onCompleted = obtainMessage(CMD_DONE_BOOLEAN, request);
                        commandInterface.multiCallControl((Integer) request.argument, onCompleted);
                        break;
                    }
                    case CMD_DONE_BOOLEAN:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception == null)
                        {
                            request.result = (Boolean) ar.result;
                        }
                        else
                        {
                            request.result = false;
                        }
                        // Wake up the requesting thread
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_HANGUP_CALL:
                    {
                        boolean hangup = false;
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception == null)
                        {
                            hangup = (Boolean) ar.result;
                        }
                        else
                        {
                            CommandException exception = (CommandException) ar.exception;
                            if (exception.e == CommandException.Error.ERR_NO_ONGOING_CALL)
                            {
                                // this means no ongoing call 250
                                WLog.d(TAG, "JonPHONE: there has not call phone");
                                hangup = true;
                                synchronized (mCallStatusArray)
                                {
                                    mCallStatusArray.clear();
                                }
                                notifyCallStateChanged();
                            }
                            else
                            {
                                hangup = false;
                            }
                        }
                        WLog.d(TAG, "JonPHONE, CMD_DONE_HANGUP_CALL:" + hangup);
                        request.result = hangup;
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_ENABLE:
                    {
                        WLog.d(TAG, "CMD_DONE_ENABLE Enter");
                        boolean enable;
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception == null)
                        {
                            enable = (Boolean) ar.result;
                        }
                        else
                        {
                            enable = false;
                        }
                        request.result = enable;
                        WLog.d(TAG, "CMD_DONE_ENABLE:" + enable);
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_DISABLE:
                    {
                        boolean disabled;
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception == null)
                        {
                            disabled = (Boolean) ar.result;
                        }
                        else
                        {
                            disabled = false;
                        }
                        request.result = disabled;
                        // Wake up the requesting thread
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_GET_NAME:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception != null)
                        {
                            request.result = "UNKNOWN";
                        }
                        else
                        {
                            request.result = ar.result;
                        }
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_GET_BONDED_DEVICE_COUNT:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception != null)
                        {
                            request.result = "UNKNOWN";
                        }
                        else
                        {
                            request.result = ar.result;
                        }
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_UNTRUSTED_LIST:
                    {
                        //				ar = (AsyncResult) msg.obj;
                        //				request = (MainThreadRequest) ar.userObj;
                        //				if (ar.exception != null)
                        //					request.result = new ArrayList<BtDevice>(0);
                        //				else
                        //					request.result = (ArrayList<BtDevice>) ar.result;
                        //				synchronized (request) {
                        //					request.notifyAll();
                        //				}
                        break;
                    }

                    case CMD_DONE_PLAYER_STATUS:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_MEDIA_META_DATA:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        request.result = (MediaInfo) ar.result;
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_CALL_STATE:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception != null)
                        {
                            request.result = new ArrayList<CallStatus>(0);
                        }
                        else
                        {
                            request.result = ar.result;
                        }
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_DEVICE_CONNECT_DISCONNECT:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception != null)
                        {
                            request.result = false;
                        }
                        else
                        {
                            request.result = (Boolean) ar.result;//handleConnctResponse((Bundle) ar.result);
                        }
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        //	handleConnctStateChanged((Bundle) request.result);
                        //	Object[] dataArr = (Object[]) request.argument;
                        //	BtDevice btDevice = (BtDevice) dataArr[0];
                        //	Boolean state = (Boolean) dataArr[1];
                        //	if (DBG)
                        //		WLog.d(TAG, "Jon: CMD_DONE_DEVICE_CONNECT_DISCONNECT:"
                        //				+ request.result);
                        //	if (((Boolean) (request.result)).booleanValue() == false) {
                        //		if (DBG)
                        //			WLog.d(TAG, "Jon: set device connect/disconnect error");
                        //	} else {
                        //		handleDeviceStateChanged(btDevice,
                        //				state ? BluetoothDevice.CONNECT_CONNECTING
                        //						: BluetoothDevice.CONNECT_DISCONNECTING);
                        //	}
                        break;
                    }
                    case CMD_DONE_START_INQUIRY:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception == null)
                        {
                            request.result = ar.result;
                        }
                        else
                        {
                            request.result = false;
                        }
                        // Wake up the requesting thread
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        if ((Boolean) request.result)
                        {
                            setInquiryMode(true);
                            new Thread()
                            {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        Thread.sleep(MAX_DISCOVERY_TIME_IN_MILLIS);
                                    }
                                    catch (InterruptedException e)
                                    {
                                    }
                                    cancelDiscovery();
                                }
                            }.start();
                        }
                        break;
                    }
                    case CMD_DONE_PALYER_GET_REPEATE_MODE:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception != null)
                        {
                            request.result = "UNKNOWN";
                        }
                        else
                        {
                            request.result = (String) ar.result;
                        }
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_GET_DISCRET_MODE:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception != null)
                        {
                            request.result = 1;
                        }
                        else
                        {
                            request.result = ar.result;
                        }
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_GET_AUTOCONN_MODE:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception != null)
                        {
                            request.result = 1;
                        }
                        else
                        {
                            request.result = ar.result;
                        }
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_GET_MIC_MUTE_STATE:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception != null)
                        {
                            request.result = 1;
                        }
                        else
                        {
                            request.result = ar.result;
                        }
                        if (DBG)
                        {
                            WLog.d(TAG, "Jon: ar.result is:" + ar.result);
                        }
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_GET_AUDIO_VOLUME:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        request.result = ar.result;
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_GET_AUDIO_RANGE:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        request.result = ar.result;
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_GET_SW_VERSION:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        if (ar.exception != null)
                        {
                            request.result = "--";
                        }
                        else
                        {
                            request.result = ar.result;
                        }
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case CMD_DONE_GET_RSSI:
                    {
                        ar = (AsyncResult) msg.obj;
                        request = (MainThreadRequest) ar.userObj;
                        request.result = ar.result;
                        synchronized (request)
                        {
                            request.notifyAll();
                        }
                        break;
                    }
                    case EVENT_DEVICE_INQUIRY:
                    {
                        ar = (AsyncResult) msg.obj;
                        handleDeviceInquiry((Bundle) ar.result);
                        break;
                    }
                    case EVENT_DEVICE_INQUIRY_FINISHED:
                        handleDeviceInquiryFinished();
                        break;
                    case EVENT_DEVICE_PAIRING_RESPONSE:
                        ar = (AsyncResult) msg.obj;
                        handlePairingResponse((Bundle) ar.result);
                        break;
                    case EVENT_DEVICE_PASSKEY_RESPONSE:
                        ar = (AsyncResult) msg.obj;
                        handlePasskeyResponse((String[]) ar.result);
                        break;
                    case EVENT_DEVICE_PLAYER_METADATA:
                        ar = (AsyncResult) msg.obj;
                        handleMediaInfoChanged((Bundle) ar.result);
                        break;
                    case EVENT_DEVICE_CALL_STATUS:
                        ar = (AsyncResult) msg.obj;
                        handleCallStateChanged((Bundle) ar.result);
                        break;
                    //			case EVENT_DEVICE_CONNECT_STATE_CHANGED: {
                    //				break;
                    //			}
                    case EVENT_BT_ENABLE_STATE_CHANGED:
                    {
                        ar = (AsyncResult) msg.obj;
                        boolean enabled = (Boolean) ar.result;
                        handleBTEnableStateChanged(enabled);
                        break;
                    }
                    case EVENT_DEVICE_REMEOVE_BONDED:
                    {
                        ar = (AsyncResult) msg.obj;
                        String deletedPairedId = null;
                        if (ar.exception == null)
                        {
                            deletedPairedId = (String) ar.result;
                        }
                        if (deletedPairedId != null)
                        {
                            handleRemovedBondedDevice(deletedPairedId);
                        }
                        break;
                    }
                    case EVENT_DEVICE_PALYER_STATUS:
                        ar = (AsyncResult) msg.obj;
                        //handlePlayerStatus((String[]) ar.result);
                        handlePlayerStatus((Bundle) ar.result);
                        break;
                    case EVENT_BT_POWER_READY:
                        ar = (AsyncResult) msg.obj;
                        handleBTPwrReady();
                        break;
                    case EVENT_BT_PLAYER_SOURCE_STATUS:
                        ar = (AsyncResult) msg.obj;
                        handleBTSourceStatusChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_HOST_ADDRES:
                        ar = (AsyncResult) msg.obj;
                        handleBTHostAddressChanged((String) ar.result);
                        break;
                    //	case CMD_DONE_GET_REMOTE_DEVICE_NAME: {
                    //		ar = (AsyncResult) msg.obj;
                    //		request = (MainThreadRequest) ar.userObj;
                    //		if (ar.exception != null)
                    //			request.result = "UNKNOWN";
                    //		else
                    //			request.result = ar.result;
                    //		synchronized (request) {
                    //			request.notifyAll();
                    //		}
                    //		break;
                    //	}
                    /////////////////////////////////////////////////////
                    case EVENT_BT_HFP_CONNECT_STATE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleHFPConnectStateChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_HFP_SERVICE_STATE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleHFPServiceStateChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_A2DP_CONNECT_STATE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleA2DPConnectStateChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_A2DP_SERVICE_STATE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleA2DPServiceStateChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_PBAP_CONNECT_STATE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handlePBAPConnectStateChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_PBAP_SERVICE_STATE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handlePBAPServiceStateChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_MAP_CONNECT_STATE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleMAPConnectStateChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_REMOTE_NAME_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleRemoteDevNameChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_PULLPB_CMT_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handlePullPBCmtChanged((ArrayList<BtPBContact>) ar.result);
                        break;
                    case EVENT_BT_GETMSG_IND_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleGetMsgIndChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_GETMSG_CMT_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleGetMsgCmtChanged();
                        break;
                    case EVENT_BT_PUSHMSG_IND_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handlePushMsgIndChanged();
                        break;
                    case EVENT_BT_PUSHMSG_CMT_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handlePushMsgCmtChanged();
                        break;
                    case EVENT_BT_MSGEVT_STAUS_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleMessageEvtStatusChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_HFAUDIO_STAUS_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleHFAudioStatusChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_LOCAL_NAME_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleLocalDevNameChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_MUTE_STATUS_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        //                        handleMuteStatusChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_PAIRED_LIST:
                        ar = (AsyncResult) msg.obj;
                        handlePairedListChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_AVRCP_CONNECT_STATE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleAVRCPConnectStateChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_PHONEBOOK_SIZE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handlePhoneBookSzChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_VOLUME_GAIN_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        //                        handleVolumeGainChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_RING_STATUS_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        //                        handleRingStatusChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_AVRCP_PLAYCMD_STATE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleAVRCPPlayCmdStatusChanged((Bundle) ar.result);
                        break;
                    case EVENT_BT_AVRCP_PAUSECMD_STATE_CHANGED:
                        ar = (AsyncResult) msg.obj;
                        handleAVRCPPauseCmdStatusChanged((Bundle) ar.result);
                        break;
                    /////////////////////////////////////////////////////
                    default:
                        WLog.w(TAG, "MainThreadHandler: unexpected message code: " + msg.what);
                        break;
                }
            }
            catch (Exception ex)
            {
                WLog.e(TAG, "MainThreadHandler: unexpected exception !!!!");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Posts the specified command to be executed on the main thread, waits for
     * the request to complete, and returns the result.
     *
     * @see
     */
    private Object sendRequest(int command, Object argument)
    {
        WLog.v("command = " + command);
        if (Looper.myLooper() == mMainThreadHandler.getLooper())
        {
            throw new RuntimeException("This method will deadlock if called from the main thread.");
        }
        MainThreadRequest request = new MainThreadRequest(argument);
        Message msg = mMainThreadHandler.obtainMessage(command, request);
        msg.sendToTarget();

        // Wait for the request to complete
        synchronized (request)
        {
            while (request.result == null)
            {
                try
                {
                    request.wait(DEFAULT_WAKE_LOCK_TIMEOUT);
                }
                catch (InterruptedException e)
                {
                    // Do nothing, go back and wait until the request is
                    // complete
                }
            }
        }
        return request.result;
    }

    void notifyPlayerMetaDataChanged()
    {
        try
        {
            Intent deviceStateIntent = new Intent(BluetoothDevice.ACTION_PLAYER_META_DATE_CHANGED);
            mContext.sendBroadcast(deviceStateIntent);
        }
        catch (Exception e)
        {
        }
    }

    void notifyPlayerStatus(final int state)
    {
        switch (state)
        {
            case BluetoothDevice.PLAYER_STATUS_PAUSE:
            case BluetoothDevice.PLAYER_STATUS_STOP:
                setPlayerState(PLAYER_STATE_PAUSE);
                break;
            case BluetoothDevice.PLAYER_STATUS_PLAY:
                setPlayerState(PLAYER_STATE_PLAY);
                break;
        }
        new Thread()
        {
            @Override
            public void run()
            {
                WLog.w(TAG, "The State of the Player in APU Utils : " + state);
                Intent deviceStateIntent = new Intent(BluetoothDevice.ACTION_PLAYER_STATUS);
                deviceStateIntent.putExtra(BluetoothDevice.EXTRA_PLAYER_STATUS, state);
                try
                {
                    mContext.sendBroadcast(deviceStateIntent);
                }
                catch (Exception e)
                {
                }
                notifyMessageToMenu(false);
            }
        }.start();
    }

    void handleBTEnableStateChanged(boolean enabled)
    {
        final int bluetoothOn = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.BLUETOOTH_ON, 0);
        WLog.d(TAG, "handleBTEnableStateChanged,enabled:" + enabled);
        if (bluetoothOn > 0)
        {
            if (enabled)
            {
                setBtHostState(BluetoothManager.STATE_ON);
            }
            else
            {
                if (!isBTStandby)
                {
                    new Thread()
                    {
                        @Override
                        public void run()
                        {
                            boolean ret = (Boolean) sendRequest(CMD_ENABLE, null);
                            if (!ret)
                            { //try again
                                sendRequest(CMD_DISABLE, null);
                            }
                        }
                    }.start();
                }
                else
                {
                    setBtHostState(BluetoothManager.STATE_OFF);
                }

            }
        }
        else
        {
            if (enabled)
            {
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        WLog.d(TAG,
                                "JonPHONE: bluetoothOn <=0 , is:" + bluetoothOn + ", or !isSystemPowerOn,try to disable bt");
                        boolean ret = (Boolean) sendRequest(CMD_DISABLE, null);
                        if (!ret)
                        { //try again
                            sendRequest(CMD_DISABLE, null);
                        }
                    }
                }.start();
            }
            else
            {
                setBtHostState(BluetoothManager.STATE_OFF);
                clearRemoteDeviceList();
            }
        }
        WLog.d(TAG, "JonDEBUG: handleBTEnableStateChanged,Exit");
    }

    void handleBTPwrReady()
    {
        if (DBG)
        {
            WLog.d(TAG, "Jon handleBTPwrReady Enter");
        }

    }

    void handleBTHostAddressChanged(String hostAddress)
    {
        WLog.d(TAG, "Jon handleBTHostAddressChanged Enter,hostAddress is:" + hostAddress);
        if (mBtHost != null)
        {
            mBtHost.address = hostAddress;
        }
    }


    boolean mIsBtSourceConnected = false;

    void handleBTSourceStatusChanged(Bundle b)
    {
        WLog.d(TAG, "Jon handleBTSourceStatusChanged Enter");
        if (b == null)
        {
            return;
        }
        String deviceSourceId = b.getString("deviceId");
        int type = b.getInt("type");
        int event = b.getInt("event");
        if (type == 2)
        {//a2dp
            switch (event)
            {
                case 6: //connected
                    mIsBtSourceConnected = true;
                    notifyBtA2DPSinkStateChanged(1);
                    break;
                case 7: //disconnected
                    mIsBtSourceConnected = false;
                    notifyBtA2DPSinkStateChanged(0);
                    break;
                default:
                    break;
            }
        }

    }

    String getCallIdbyPhoneNumber(String number)
    {
        return number;
    }

    void handleCallStateChanged(Bundle b)
    {
        WLog.d(TAG, "Jon: [BT] handleCallStateChanged Enter");
        int status = b.getInt("status");
        int call_idx = b.getInt("call_idex");
        int direction = b.getInt("direction");
        int mode = b.getInt("mode");
        int multiparty = b.getInt("multiparty");
        int number_type = b.getInt("number_type");
        String number = b.getString("number");

        CallStatus call = new CallStatus();
        call.callIndex = call_idx;
        call.phoneNumber = number;
        call.callMode = mode;
        call.callState = status;
        call.callDir = direction;
        call.callConf = multiparty;
        call.pnType = number_type;
        call.callerId = getCallIdbyPhoneNumber(number);
        call.activeStartTime = -1;

        if (needResetCallArray)
        {
            needResetCallArray = false;
            WLog.d(TAG, "Jon: [BT] reset Call Statuse Array");
            synchronized (mCallStatusArray)
            {
                mCallStatusArray.clear();
            }
        }

        synchronized (mCallStatusArray)
        {
            if (mCallStatusArray != null && mCallStatusArray.size() > 0)
            {
                boolean inCallArray = false;
                for (int j = 0; j < mCallStatusArray.size(); j++)
                {
                    CallStatus callItem = mCallStatusArray.get(j);
                    if (callItem.callIndex == call.callIndex)
                    {
                        inCallArray = true;
                        if (callItem.phoneNumber.equals(call.phoneNumber))
                        {
                            if (call.callState == BTConstants.BT_CALL_STATE_ACTIVE || call.callState == BTConstants.BT_CALL_STATE_HELD)
                            {
                                if (callItem.activeStartTime != -1)
                                {
                                    call.activeStartTime = callItem.activeStartTime;
                                }
                                else
                                {
                                    call.activeStartTime = SystemClock.elapsedRealtime(); //System.currentTimeMillis();
                                }
                            }
                            mCallStatusArray.remove(j);
                            mCallStatusArray.add(call);
                            mCurrentCall = call;
                            break;
                        }
                        else
                        {
                            mCallStatusArray.remove(j);
                            if (call.callState == BTConstants.BT_CALL_STATE_ACTIVE)
                            {
                                call.activeStartTime = SystemClock.elapsedRealtime();
                            }
                            mCallStatusArray.add(call);
                            mCurrentCall = call;
                        }
                    }
                }
                if (!inCallArray)
                {
                    if (call.callState == BTConstants.BT_CALL_STATE_ACTIVE)
                    {
                        call.activeStartTime = SystemClock.elapsedRealtime();
                    }
                    mCallStatusArray.add(call);
                    mCurrentCall = call;
                }
            }
            else
            {
                mCallStatusArray = new ArrayList<CallStatus>();
                if (call.callState == BTConstants.BT_CALL_STATE_ACTIVE)
                {
                    call.activeStartTime = SystemClock.elapsedRealtime();
                }
                mCallStatusArray.add(call);
                mCurrentCall = call;
            }
        }
        WLog.d(TAG, "Jon: [BT] handleCallStateChanged mCallStatusArray size:" + mCallStatusArray.size());
        for (int j = 0; j < mCallStatusArray.size(); j++)
        {
            CallStatus callItem = mCallStatusArray.get(j);
            WLog.d(TAG, "Jon: [BT] handleCallStateChanged call:" + j + ",is:" + callItem);
        }
        notifyCallStateChanged();
        if (mCallStatusArray.size() == 1 &&
                mCallStatusArray.get(0).callState == BTConstants.BT_CALL_STATE_HELD &&
                mHFPState == BtDeviceRemote.HFP_TL_TWCALLONHoldNoActive)
        {
            new Thread()
            {
                @Override
                public void run()
                {
                    WLog.d(TAG, "Jon: [BT] call held now, need change to active");
                    sendCommand("AT+B HFMCAL 2,0");
                }
            }.start();
        }
    }


    void notifyCallStateChanged()
    {
        int state = getCurrentCallState();
        WLog.d(TAG, "Jon: [BT] notifyCallStateChanged Enter, state is: " + state);
        mLastCallState = mCurrentCallState;
        mCurrentCallState = state;
        mLastCallSz = mCurrentCallSz;
        mCurrentCallSz = mCallStatusArray.size();
        WLog.d(TAG, "Jon: [BT] notifyCallStateChanged Enter, mLastCallState is: " + mLastCallState +
                ",mCurrentCallState is:" + mCurrentCallState + ",mIncallBeforeConnection is:" + mIncallBeforeConnection +
                ",mCallStatusArray.size():" + mCallStatusArray.size() +
                ",mLastCallSz is:" + mLastCallSz +
                ",mCurrentCallSz is:" + mCurrentCallSz);
        if ((mCurrentCallState == BluetoothDevice.CALL_STATE_OFFHOOK && mLastCallState == BluetoothDevice.CALL_STATE_IDLE) || (mCurrentCallState == BluetoothDevice.CALL_STATE_OFFHOOK &&
                mLastCallState == BluetoothDevice.CALL_STATE_OFFHOOK &&
                ((mCurrentCallSz == 1) && (mLastCallSz == 1))))
        {
            //it is maybe recorder or play use HFP,also maybe call before new connect
            if (!mIncallBeforeConnection && (System.currentTimeMillis() - mHFPConnectionTimeStamp) > 2 * 1000)
            { //not call before new connect
                WLog.d(TAG, "Jon: [BT] notifyCallStateChanged Enter,no need tell app");
                return;
            }
        }
        mIncallBeforeConnection = false;

        if (state == 0)
        {
            mMicUnMuteState = 1;
        }

        synchronized (mRecords)
        {
            for (int i = mRecords.size() - 1; i >= 0; i--)
            {
                Record r = mRecords.get(i);
                if ((r.events & BluetoothListener.LISTEN_CALL_STATE) != 0)
                {
                    try
                    {
                        r.callback.onCallStateChanged(state);
                    }
                    catch (RemoteException ex)
                    {
                        remove(r.binder);
                    }
                }
            }
        }
        Intent deviceCallIntent = new Intent(BluetoothDevice.ACTION_PHONE_STATE_CHANGED);
        deviceCallIntent.putExtra(BluetoothDevice.EXTRA_PHONE_STATE, state);
        try
        {
            mContext.sendBroadcast(deviceCallIntent);
        }
        catch (Exception e)
        {
        }
        if (mCurrenAddr != null && mCurrenAddr.length() > 0)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.onCallStateChanged(state);
            }
        }
    }

    int getCurrentCallState()
    {
        if (isIdleCall())
        {
            return BluetoothDevice.CALL_STATE_IDLE;
        }
        else if (hasActiveRingingCall())
        {
            return BluetoothDevice.CALL_STATE_RINGING;
        }
        else if (hasActiveDialingCall())
        {
            return BluetoothDevice.CALL_STATE_DIALING;
        }
        else
        {
            return BluetoothDevice.CALL_STATE_OFFHOOK;
        }
    }

    private CallStatus getFirstActiveCall()
    {
        CallStatus status = null;
        synchronized (mCallStatusArray)
        {
            for (int i = 0; i < mCallStatusArray.size(); i++)
            {
                if (ApuBtUtils.convertCallStateToDeviceCallState(
                        mCallStatusArray.get(i).callState) == BluetoothDevice.CALL_STATE_OFFHOOK)
                {
                    status = mCallStatusArray.get(i);
                    break;
                }
            }
        }
        return status;
    }


    private CallStatus getFirstActiveRingingCall()
    {
        CallStatus status = null;
        synchronized (mCallStatusArray)
        {
            for (int i = 0; i < mCallStatusArray.size(); i++)
            {
                if (ApuBtUtils.convertCallStateToDeviceCallState(
                        mCallStatusArray.get(i).callState) == BluetoothDevice.CALL_STATE_RINGING)
                {
                    status = mCallStatusArray.get(i);
                    break;
                }
            }
        }
        return status;
    }

    private boolean hasActiveRingingCall()
    {
        CallStatus status = null;
        synchronized (mCallStatusArray)
        {
            for (int i = 0; i < mCallStatusArray.size(); i++)
            {
                if (ApuBtUtils.convertCallStateToDeviceCallState(
                        mCallStatusArray.get(i).callState) == BluetoothDevice.CALL_STATE_RINGING)
                {
                    status = mCallStatusArray.get(i);
                    break;
                }
            }
        }
        return (status != null);
    }

    private boolean hasActiveDialingCall()
    {
        CallStatus status = null;
        synchronized (mCallStatusArray)
        {
            for (int i = 0; i < mCallStatusArray.size(); i++)
            {
                if (ApuBtUtils.convertCallStateToDeviceCallState(
                        mCallStatusArray.get(i).callState) == BluetoothDevice.CALL_STATE_DIALING)
                {
                    status = mCallStatusArray.get(i);
                    break;
                }
            }
        }
        return (status != null);
    }

    private boolean hasOffHookCall()
    {
        CallStatus status = null;
        synchronized (mCallStatusArray)
        {
            for (int i = 0; i < mCallStatusArray.size(); i++)
            {
                if (ApuBtUtils.convertCallStateToDeviceCallState(
                        mCallStatusArray.get(i).callState) == BluetoothDevice.CALL_STATE_OFFHOOK)
                {
                    status = mCallStatusArray.get(i);
                    break;
                }
            }
        }
        return (status != null);
    }

    private boolean isIdleCall()
    {
        boolean isIdle = true;
        synchronized (mCallStatusArray)
        {
            WLog.d(TAG, "JonPHONE: isIdleCall Enter ,mCallStatusArray.size():" + mCallStatusArray.size());
            if (mCallStatusArray == null || mCallStatusArray.size() == 0)
            {
                return isIdle;
            }
            for (int i = 0; i < mCallStatusArray.size(); i++)
            {
                WLog.d(TAG, "JonPHONE i:" + i + " call is:" + mCallStatusArray.get(i));
                if (ApuBtUtils.convertCallStateToDeviceCallState(
                        mCallStatusArray.get(i).callState) != BluetoothDevice.CALL_STATE_IDLE)
                {
                    isIdle = false;
                    break;
                }
            }
        }
        return isIdle;
    }

    private void onBtEnable()
    {
        new Thread()
        {
            public void run()
            {
                WLog.d(TAG, "onBtEnable Enter");
                clearRemoteDeviceList();
                releaseRingPopCheckTimer();
                synchronized (mMediaInfo)
                {
                    mMediaInfo.reset();
                    mLastMusicTile = null;
                    notifyPlayerMetaDataChanged();
                }
                getSwVersion();
                getTrustedList();
                setScanMode(BluetoothManager.SCAN_MODE_CONNECTABLE, 0);
                String device_name = Settings.System
                        .getString(mContext.getContentResolver(), BluetoothSettings.BLUETOOTH_DEVICE_NAME);
                WLog.d(TAG, "onBtEnable BT device name:" + device_name);
                if (device_name == null)
                {
                    device_name = mContext.getString(R.string.bt_default_name);
                }
                sendRequest(CMD_SET_NAME, device_name);
                sendCommand("AT+B GLBD");//get local host address;
                WLog.d(TAG, "Jon: [BT] onBtEnable Exit!");
            }
        }.start();
    }

    void disconnectAllDevice()
    {
        ArrayList<BtDeviceRemote> deviceList = new ArrayList<BtDeviceRemote>();
        synchronized (mRemoteDeviceMap)
        {
            Collection<BtDeviceRemote> devices = mRemoteDeviceMap.values();
            Iterator<BtDeviceRemote> iterator = devices.iterator();
            while (iterator.hasNext())
            {
                BtDeviceRemote btDevice = (BtDeviceRemote) iterator.next();
                if (btDevice != null)
                {
                    // btDevice.connect(false);
                    deviceList.add(btDevice);
                }
            }
        }
        for (int i = 0; i < deviceList.size(); i++)
        {
            try
            {
                deviceList.get(i).connect(false);
            }
            catch (Exception ex)
            {
            }
        }
    }

    private void onBtDisable()
    {
        new Thread()
        {
            public void run()
            {
                WLog.d(TAG, "Jon: [BT] onBtDisable");
                releaseRingPopCheckTimer();
                synchronized (mCallStatusArray)
                {
                    mCallStatusArray.clear();
                }
                mLastCallSz = 0;
                mCurrentCallSz = 0;
                notifyCallStateChanged();
                synchronized (mMediaInfo)
                {
                    mMediaInfo.reset();
                    mLastMusicTile = null;
                    notifyPlayerMetaDataChanged();
                    notifyMessageToMenu(true);
                }
                clearRemoteDeviceList();
                mCurrenAddr = null;
                mHFPState = -1;
                mA2DPState = -1;
                mPBAPState = -1;
                mHFPConnectionTimeStamp = 0;
                mIsInCallState = false;
                mIncallBeforeConnection = false;
                mCurrentCallState = BluetoothDevice.CALL_STATE_IDLE;
                mLastCallState = BluetoothDevice.CALL_STATE_IDLE;
                WLog.d(TAG, "Jon: [BT] onBtDisable Exit");
            }
        }.start();
    }


    private final Runnable mTurningOnBtTimout = new Runnable()
    {
        public void run()
        {
            //try again
            int hostState = getBtHostState();
            WLog.d(TAG, "JonDEBUG: mTurningOnBtTimout Enter,hostState:" + hostState);
            if (hostState == BluetoothManager.STATE_TURNING_ON)
            { //time out
                WLog.d(TAG, "JonDEBUG: TURNING ON TIME OUT");
                //                int accState = iPeripheral.getACCStatus();
                //                if ((accState > 0) && (mLastPowerStatus == PeripheralManager.MCU_STATUS_POWER_ON || mLastPowerStatus == PeripheralManager.MCU_STATUS_POWER_ON_POST_STANDBY))
                //                {
                //                    int bluetoothOn = Settings.Global
                //                            .getInt(mContext.getContentResolver(), Settings.Global.BLUETOOTH_ON, 0);
                //                    if (bluetoothOn == 1)
                //                    {
                //                        WLog.d(TAG, "JonPHONE: TURNING ON TIME OUT,retry turning on again");
                //                        setBtHostState(BluetoothManager.STATE_TURNING_ON);
                //                        boolean ret = (Boolean) sendRequest(CMD_ENABLE, null);
                //                    }
                //                }
                //                else
                //                {
                WLog.d(TAG, "JonPHONE: TURNING ON TIME OUT,retry turning off because acc = 0");
                disable(false);
                //                }
            }
        }
    };

    private final Runnable mTurningOffBtTimout = new Runnable()
    {
        public void run()
        {
            int hostState = getBtHostState();
            WLog.d(TAG, "JonDEBUG: mTurningOffBtTimout Enter,hostState:" + hostState);
            if (hostState == BluetoothManager.STATE_TURNING_OFF)
            { //time out
                WLog.d(TAG, "JonDEBUG: TURNING OFF TIME OUT");
                //                int accState = iPeripheral.getACCStatus();
                //                if ((accState > 0) && (mLastPowerStatus == PeripheralManager.MCU_STATUS_POWER_ON || mLastPowerStatus == PeripheralManager.MCU_STATUS_POWER_ON_POST_STANDBY))
                //                {
                //                    int bluetoothOn = Settings.Global
                //                            .getInt(mContext.getContentResolver(), Settings.Global.BLUETOOTH_ON, 0);
                //                    if (bluetoothOn == 1)
                //                    {
                //                        WLog.d(TAG, "JonDEBUG: TURNING OFF TIME OUT,retry turn on becase bluetooth on");
                //                        enable();
                //                    }
                //                    else
                //                    {
                //                        WLog.d(TAG, "JonDEBUG: TURNING OFF TIME OUT,retry turn on becase bluetooth off");
                //                        setBtHostState(BluetoothManager.STATE_TURNING_OFF);
                //                        boolean ret = (Boolean) sendRequest(CMD_DISABLE, null);
                //                    }
                //                }
                //                else
                //                {
                WLog.d(TAG, "JonPHONE: TURNING off TIME OUT,retry turning off because acc = 0");
                setBtHostState(BluetoothManager.STATE_TURNING_OFF);
                boolean ret = (Boolean) sendRequest(CMD_DISABLE, null);
                //                }
            }
        }
    };


    void setBtHostState(int state)
    {
        WLog.d(TAG, "JonDEBUG: setBtHostState state:" + state);
        if (state == BluetoothManager.STATE_OFF)
        {
            sBTCheckEnableStatus.removeCallbacks(mTurningOnBtTimout);
            sBTCheckEnableStatus.removeCallbacks(mTurningOffBtTimout);
            onBtDisable();
        }
        else if (state == BluetoothManager.STATE_ON)
        {
            sBTCheckEnableStatus.removeCallbacks(mTurningOnBtTimout);
            sBTCheckEnableStatus.removeCallbacks(mTurningOffBtTimout);
            onBtEnable();
        }
        else if (state == BluetoothManager.STATE_TURNING_ON)
        {
            sBTCheckEnableStatus.removeCallbacks(mTurningOnBtTimout);
            sBTCheckEnableStatus.postDelayed(mTurningOnBtTimout, 10000);
        }
        else if (state == BluetoothManager.STATE_TURNING_OFF)
        {
            disconnectAllDevice();
            sBTCheckEnableStatus.removeCallbacks(mTurningOffBtTimout);
            sBTCheckEnableStatus.postDelayed(mTurningOffBtTimout, 10000);
        }
        synchronized (mBtHost)
        {
            if (mBtHost.setState(state))
            {
                notifyBtStateChanged();
            }
        }
        WLog.d(TAG, "JonDEBUG: setBtHostState Exit");
    }

    int getBtHostState()
    {
        //	synchronized (mBtHost) {
        return mBtHost.getState();
        //	}
    }

    void handleMediaInfoChanged(Bundle b)
    {
        updateMediaInfo(b);
    }

    void handleRemovedBondedDevice(String deviceId)
    {
        //	try {
        //	BtDevice btDevice = findBtDeviceByPairedId(deviceId);
        //	synchronized (mAddrDeviceMap) {
        //		removeDeviceList(btDevice.address);
        //	}
        //	setDevicePairingState(
        //			btDevice,
        //			ApuBtUtils.convertBondToPairingState(BluetoothDevice.BOND_NONE),
        //			true);
        //	if(btDevice != null)
        //		btDevice.resetPhoneBook();
        //	} catch (Exception ex) {
        //	}
    }


    //BtDevice findBtDeviceByInquiredId(String inquiredId) {
    //	synchronized (mAddrDeviceMap) {
    //		Collection<BtDevice> devices = mAddrDeviceMap.values();
    //		Iterator<BtDevice> iterator = devices.iterator();
    //		while (iterator.hasNext()) {
    //			BtDevice btDevice = (BtDevice) iterator.next();
    //			if (inquiredId.equals(btDevice.inquiredId))
    //				return btDevice;
    //		}
    //	}
    //	return null;
    //}


    //BtDevice findBtDeviceBySourceId(String sourceId) {
    //	synchronized (mAddrDeviceMap) {
    //		Collection<BtDevice> devices = mAddrDeviceMap.values();
    //		Iterator<BtDevice> iterator = devices.iterator();
    //		while (iterator.hasNext()) {
    //			BtDevice btDevice = (BtDevice) iterator.next();
    //			if (sourceId != null && sourceId.equals(btDevice.getSourceId()))
    //				return btDevice;
    //		}
    //	}
    //	return null;
    //}

    void handlePasskeyResponse(final String[] result)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                // TODO ask user to enter the pass-key
                try
                {
                    Thread.sleep(2500);
                }
                catch (InterruptedException e)
                {
                }
                String deviceId = result[0];

                // TODO depending on scanMode accept the request
                int ioCapability = Integer.parseInt(result[2]);
                String passKey;
                if (ioCapability == BTConstants.BT_PAIRING_IO_CAPABILITY_DISPLAY_YES_NO && mPairingMode == BTConstants.BT_PAIRING_MODE_FORWARD)
                {
                    passKey = result[3];
                }
                else
                {
                    passKey = null;
                }
                Object[] data = new Object[2];
                data[0] = deviceId;
                data[1] = passKey;

                // sendRequest(CMD_ACCPET_INCOMING_PASSKEY_PAIRING, data);
            }
        }.start();
    }

    void handleDeviceInquiry(Bundle btDevice)
    {
        WLog.v("handleDeviceInquiry...");
        //        synchronized (mAddrDeviceMap)
        //        {
        //            addDeviceList(btDevice);
        //        }
        BluetoothDevice device = BluetoothManager.getDefault(mContext).getRemoteDevice(btDevice.getString("mac"));
        notifyDeviceFound(device);
    }

    void handleDeviceInquiryFinished()
    {
        //	new Thread() {
        //		@Override
        //		public void run() {
        //			ArrayList<BtDevice> arrayList = getTrustedList();
        //			handleTrustedDevices(arrayList);
        //
        //				arrayList = getUntrustedList();
        //				handleUntrustedDevices(arrayList);
        //			}
        //		}.start();
        WLog.v(TAG, "scan finish...");
        Intent intent = new Intent(BluetoothManager.ACTION_DISCOVERY_FINISHED);
        mContext.sendBroadcast(intent);
    }

    void setInquiryMode(boolean started)
    {
        WLog.v(TAG, "setInquiryMode " + started);
        synchronized (mBtHost)
        {
            mBtHost.isInquiring = started;
        }
        Intent discoveryIntent;
        if (started)
        {
            discoveryIntent = new Intent(BluetoothManager.ACTION_DISCOVERY_STARTED);
        }
        else
        {
            discoveryIntent = new Intent(BluetoothManager.ACTION_DISCOVERY_FINISHED);
        }
        try
        {
            mContext.sendBroadcast(discoveryIntent);
        }
        catch (Exception e)
        {
        }
    }

    void notifyDeviceFound(BluetoothDevice device)
    {
        Intent deviecFoundIntent = new Intent(BluetoothDevice.ACTION_FOUND);
        deviecFoundIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        try
        {
            mContext.sendBroadcast(deviecFoundIntent);
        }
        catch (Exception e)
        {
        }
    }

    void handlePairingResponse(Bundle b)
    {
        if (DBG)
        {
            WLog.d(TAG, "handlePairingResponse Enter");
        }
        int result = b.getInt("result");
        String address = b.getString("bd");
        if (!isValidMacAddress(address))
        {
            return;
        }
        if (result == 0)
        {//paired
            if (mRemoteDeviceMap.get(address) == null)
            {
                BtDeviceRemote device = new BtDeviceRemote(address);
                device.setBondState(BluetoothDevice.BOND_BONDED);
                addRemoteDeviceList(device);
                notifyBondStateChanged(address, BluetoothDevice.BOND_BONDED);
            }
        }
        else
        {
            if (mRemoteDeviceMap.get(address) != null)
            {
                removeRemoteDeviceList(address);
            }
            notifyBondStateChanged(address, BluetoothDevice.BOND_NONE);
        }
    }

    //	void setDevicePairingState(BtDevice device, int pairingState,
    //			final boolean notify) {
    //	}

    //    void setDeviceBondState(String address, int pairingState) {
    //	}

    void notifyBondStateChanged(String address, int State)
    {
        Intent deviecBondIntent = new Intent(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        deviecBondIntent.putExtra(BluetoothDevice.EXTRA_BOND_STATE, State);
        BluetoothDevice btDevice = null;
        if (address != null)
        {
            btDevice = BluetoothManager.getDefault(mContext).getRemoteDevice(address);
        }
        deviecBondIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, btDevice);
        if (State == BluetoothDevice.BOND_NONE)
        {
            int reason = BluetoothDevice.UNBOND_REASON_REMOVED;
            //if (State == 4) {
            //	reason = BluetoothDevice.UNBOND_REASON_MAX_DEVICE_REACHED;
            //}
            deviecBondIntent.putExtra(BluetoothDevice.EXTRA_REASON, reason);
        }
        try
        {
            mContext.sendBroadcast(deviecBondIntent);
        }
        catch (Exception e)
        {
        }
    }

    private void notifyBtStateChanged()
    {
        try
        {
            Intent deviecBondIntent = new Intent(BluetoothManager.ACTION_STATE_CHANGED);
            deviecBondIntent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            deviecBondIntent.putExtra(BluetoothManager.EXTRA_STATE, mBtHost.currState);
            deviecBondIntent.putExtra(BluetoothManager.EXTRA_PREVIOUS_STATE, mBtHost.prevState);
            mContext.sendStickyBroadcast(deviecBondIntent);
        }
        catch (Exception e)
        {
        }
    }

    boolean setPairingMode(int pairingMode, String pinCode)
    {
        Object[] data = new Object[2];
        data[0] = pairingMode;
        data[1] = pinCode;
        return (Boolean) sendRequest(CMD_SET_PAIRING_MODE, data);
    }

    /**
     * Asynchronous ("fire and forget") version of sendRequest(): Posts the
     * specified command to be executed on the main thread, and returns
     * immediately.
     *
     * @see
     */
    private void sendRequestAsync(int command)
    {
        mMainThreadHandler.sendEmptyMessage(command);
    }

    private static BtManagerService sMe;

    public static BtManagerService getDefault()
    {
        return sMe;
    }

    public boolean isWifiConnected(Context context)
    {
        if (context != null)
        {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWiFiNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (mWiFiNetworkInfo != null)
            {
                return mWiFiNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    boolean mLocationServiceStarted = false;

    public boolean isGPSEnabled()
    {
        boolean isEnabled = false;
        try
        {
            isEnabled = getLocationManager().isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        catch (Exception e)
        {
        }
        return isEnabled;

    }

    public void startLocationService()
    {
        if (!mLocationServiceStarted)
        {
            mLocationServiceStarted = true;
            //            getLocationManager().addNmeaListener(mNmeaListener);
        }
    }

    public void stopLocationService()
    {
        getLocationManager().removeNmeaListener(mNmeaListener);
        mLocationServiceStarted = false;
    }

    GpsStatus.NmeaListener mNmeaListener = new GpsStatus.NmeaListener()
    {
        public void onNmeaReceived(long timestamp, String nmea)
        {
            if (getAutoTime())
            {
                handlerNmea(nmea);
            }
        }
    };

    void handlerNmea(String nmea)
    {
        //        if (Settings.System.getInt(mContext.getContentResolver(), Settings.System.AUTO_TIME_GPS, 0) == 1 && nmea
        //                .startsWith("$GPRMC"))
        //        {
        //            adjustFromGpsUTCTime(nmea);
        //        }
    }

    void adjustFromGpsUTCTime(String gprmc)
    {
        try
        {
            String[] gprmcArray = gprmc.split("\\,");
            String hh = gprmcArray[1].substring(0, 2);
            String mm = gprmcArray[1].substring(2, 4);
            String ss = gprmcArray[1].substring(4, 6);
            String day = gprmcArray[9].substring(0, 2);
            String month = gprmcArray[9].substring(2, 4);
            String year = "20" + gprmcArray[9].substring(4, 6);

            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.set(Calendar.YEAR, Integer.valueOf(year));
            c.set(Calendar.MONTH, Integer.valueOf(month) - 1);
            c.set(Calendar.DAY_OF_MONTH, Integer.valueOf(day));
            c.set(Calendar.HOUR_OF_DAY, Integer.valueOf(hh));
            c.set(Calendar.MINUTE, Integer.valueOf(mm));
            c.set(Calendar.SECOND, Integer.valueOf(ss));

            long when = c.getTimeInMillis();

            if (when / 1000 < Integer.MAX_VALUE)
            {
                long gained = c.getTimeInMillis() - System.currentTimeMillis();
                long timeSinceLastUpdate = SystemClock.elapsedRealtime() - mSavedAtTime;
                if ((mSavedAtTime == 0) || (timeSinceLastUpdate > mTimeUpdateSpacing) || (Math
                        .abs(gained) > mTimeUpdateDiff))
                {
                    WLog.d(TAG, "JONGPS " + c.getTimeZone()
                            .getDisplayName() + " :" + year + "-" + month + "-" + day + " " + hh + ":" + mm + ":" + ss);
                    WLog.d(TAG, "JONGPS when:" + when);
                    SystemClock.setCurrentTimeMillis(when);
                    mSavedAtTime = SystemClock.elapsedRealtime();
                    Intent intent = new Intent(Intent.ACTION_DATE_CHANGED);
                    mContext.sendStickyBroadcast(intent);
                }
            }
        }
        catch (Exception e)
        {
            //WLog.d(TAG, "JONGPS Error!!!!");
        }
    }

    public LocationManager getLocationManager()
    {
        if (mLocationManager == null)
        {
            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        }
        return mLocationManager;
    }

    //    int mLastPowerStatus = PeripheralManager.MCU_STATUS_POWER_OFF;

    private final Runnable mCheckPowerRunnable = new Runnable()
    {
        public void run()
        {
            // sleep 100 ms
            //            int accState = iPeripheral.getACCStatus();
            //            WLog.d(TAG, "JonPHONE: mLastPowerStatus: " + mLastPowerStatus + ",accState:" + accState);
            //            if ((accState > 0) && (mLastPowerStatus == PeripheralManager.MCU_STATUS_POWER_ON || mLastPowerStatus == PeripheralManager.MCU_STATUS_POWER_ON_POST_STANDBY) && (TBoxManager
            //                    .getDefault().getECallReportState() != TBoxManager.ECALL_STATE_IN))
            //            {
            //                int bluetoothOn = Settings.Global
            //                        .getInt(mContext.getContentResolver(), Settings.Global.BLUETOOTH_ON, 0);
            //                if (bluetoothOn == 1)
            //                {
            //                    WLog.d(TAG, "JonPHONE: enable()");
            //                    enable();
            //                }
            //            }
            //            else
            //            {
            WLog.d(TAG, "JonPHONE: disable()");
            disable(false);
            //            }
        }
    };


    void checkPowerStatus()
    {
        WLog.d(TAG, "JonPHONE: checkPowerStatus Enter");
        synchronized (sBTCheckSysPower)
        {
            sBTCheckSysPower.removeCallbacks(mCheckPowerRunnable);
            sBTCheckSysPower.postDelayed(mCheckPowerRunnable, 1500);
        }
        WLog.d(TAG, "JonPHONE: checkPowerStatus Exit");
    }

    void checkIncallState()
    {
        //        new Thread()
        //        {
        //            public void run()
        //            {
        //                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        //                if (getBtHostState() == BluetoothManager.STATE_ON && getCallState() != BluetoothDevice.CALL_STATE_IDLE)
        //                {
        //                    WLog.d(TAG, "JonPHONE: power on,phone is incall");
        //                    triggerBTPhoneSource();
        //                }
        //            }
        //        }.start();
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            WLog.d(TAG, "mReceiver received action:" + action);
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
            {
                ConnectivityManager connectMgr = (ConnectivityManager) mContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (!wifiNetInfo.isConnected()/* && !mobNetInfo.isConnected() */)
                {
                    WLog.i(TAG, "Sntp network unconnect");
                }
                else
                {
                    // connect network
                    WLog.i(TAG, "Sntp network connected");
                    //updateTimeBySntp();
                }
            }
            else if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(action))
            {
                /*
                LocationManager lm = getLocationManager();
				if (isGPSEnabled()) {
					startLocationService();
				}
                */
            }
            //            else if (Intent.ACTION_POWER_STATE_CHANGED.equals(action))
            //            {
            //                int state = intent.getIntExtra(Intent.EXTRA_DEVICE_POWER_STATE, PeripheralManager.MCU_STATUS_POWER_OFF);
            //                WLog.d(TAG, "JonPHONE: Power state:" + state + ",call state is:" + getCallState());
            //                if (state == PeripheralManager.MCU_STATUS_POWER_ON)
            //                {
            //                    WLog.d(TAG, "JonPHONE: MCU_STATUS_POWER_ON");
            //                    mLastPowerStatus = state;
            //                    checkPowerStatus();
            //                    checkIncallState();
            //                }
            //                else if (state == PeripheralManager.MCU_STATUS_POWER_ON_POST_STANDBY)
            //                {
            //                    WLog.d(TAG, "JonPHONE: MCU_STATUS_POWER_ON_POST_STANDBY");
            //                    mLastPowerStatus = state;
            //                    checkPowerStatus();
            //                }
            //                else if (state == PeripheralManager.MCU_STATUS_POWER_ON_STANDBY)
            //                {
            //                    WLog.d(TAG, "JonPHONE: MCU_STATUS_POWER_ON_STANDBY");
            //                    mLastPowerStatus = state;
            //                    checkPowerStatus();
            //                }
            //                else if (state == PeripheralManager.MCU_STATUS_POWER_OFF)
            //                {
            //                    WLog.d(TAG, "JonPHONE: MCU_STATUS_POWER_OFF");
            //                    mLastPowerStatus = state;
            //                    checkPowerStatus();
            //                }
            //            }
            else if ("com.chleon.vehicle.status".equals(action))
            {
                WLog.d(TAG, "JonAmLink: com.chleon.vehicle.status");
                notifyParkStatus();
            }
            else if ("WM_AMLINK_START".equals(action))
            {
                WLog.d(TAG, "JonAmLink: WM_AMLINK_START");
                notifyParkStatus();
            }
            else if ("WM_AMLINK_STS_SHOW".equals(action))
            {
                WLog.d(TAG, "JonAmLink: WM_AMLINK_STS_SHOW");
                if (GetConnectDeviceAddr(BtDeviceRemote.SVC_A2DP_SNK) != null)
                {
                    new Thread()
                    {
                        @Override
                        public void run()
                        {
                            listenBTSource();
                        }
                    }.start();
                }
            }
            else if ("WM_AMLINK_STS_HIDE".equals(action))
            {
                WLog.d(TAG, "JonAmLink: WM_AMLINK_STS_HIDE");
                /*
                new Thread() {
			        @Override
			        public void run() {
			            if (mMusicAction == ACTION_PLAY_STOP) {
						    exitBTSource();
			            }
			        }
		         }.start();
                 */
            }
            //            else if ("com.chleon.accstate.changed".equals(action))
            //            {
            //                int state = intent.getIntExtra("state", 0);
            //                checkPowerStatus();
            //                if (state > 0)
            //                {
            //                    WLog.d(TAG, "JonPHONE: ACC > 0");
            //                    if (state == 1)
            //                    {
            //                        checkIncallState();
            //                    }
            //                }
            //                else
            //                {
            //                    WLog.d(TAG, "JonPHONE: ACC = 0");
            //                }
            //            }
            else if ("com.chleon.user.confirmed".equals(action))
            {
                WLog.d(TAG, "JonPHONE: com.chleon.user.confirmed");
                isUserConfirmed = true;
            }
            //            else if (BabeUtils.ACTION_REVERSE_MODE_CHANGED.equals(action))
            //            {
            //                //int reverseStatus = intent.getIntExtra("status", -1);
            //                //  boolean isReverse = iPeripheral.isRevereOn();
            //                //	if(!isReverse) {
            //                //		WLog.d(TAG,"JonPHONE: Reverse OFF, checkIncallState");
            //                //	    checkIncallState();
            //                //	}
            //                int productType = BabeUtils.getProductType();
            //                if (productType == BabeUtils.TYPE_PRODUCT_AUTOLINK_XM1001 ||
            //                        productType == BabeUtils.TYPE_PRODUCT_AUTOLINK_D50 ||
            //                        productType == BabeUtils.TYPE_PRODUCT_AUTOLINK_C50)
            //                {
            //                    boolean isReverse = iPeripheral.isReverseOn();
            //                    if (isReverse)
            //                    {
            //                        boolean isIdle = BluetoothManager.getDefault().isIdle();
            //                        WLog.d(TAG, "JonPHONE: Reverse on, BtPhone idle is " + isIdle);
            //                        if (!isIdle)
            //                        {
            //                            if (mPhoneHFAudioConnected > 0)
            //                            {
            //                                setPhonePrivateMode(0);
            //                            }
            //                        }
            //                    }
            //                }
            //            }
            //            else if ("android.intent.action.SOURCE_CHANGED".equals(action))
            //            {
            //                int source = intent.getIntExtra("current", -1);
            //                if (source == UserInputSource.ID_BLUETOOTH_MUSIC)
            //                {
            //                    notifyMessageToMenu(true);
            //                }
            //            }
            else if (Intent.ACTION_BOOT_COMPLETED.equals(action))
            {
            }
            //            else if (TBoxManager.ACTION_INTENT_ECALL_STATE.equals(action))
            //            {
            //                checkPowerStatus();
            //            }
            else if (ACTION_SYS_GOTO_SLEEP.equals(action))
            {
                gotoSleep();
            }
        }
    };


    void gotoSleep()
    {
        WLog.d(TAG, "gotoSleep Enter");
        new Thread()
        {
            @Override
            public void run()
            {
                synchronized (sBTCheckSysPower)
                {
                    sBTCheckSysPower.removeCallbacks(mCheckPowerRunnable);
                }
                disable(false);
            }
        }.start();
    }

    void notifyParkStatus()
    {
        //        if (iPeripheral != null)
        //        {
        //            int speed = iPeripheral.getVelocity();
        //            WLog.d(TAG, "notifyParkStatus Enter, speed is: " + speed);
        //            if (speed > 3)
        //            {
        //                Intent intent = new Intent();
        //                intent.setAction("WM_AMLINK_PARKING_OFF");
        //                try
        //                {
        //                    mContext.sendBroadcast(intent);
        //                }
        //                catch (Exception e)
        //                {
        //                }
        //            }
        //            else
        //            {
        //                Intent intent = new Intent();
        //                intent.setAction("WM_AMLINK_PARKING_ON");
        //                try
        //                {
        //                    mContext.sendBroadcast(intent);
        //                }
        //                catch (Exception e)
        //                {
        //                }
        //            }
        //        }
    }

    private void showAmLink(boolean show)
    {
        if (show)
        {
            Intent intent = new Intent();
            intent.setAction("WM_AMLINK_WND_SHOW");
            try
            {
                mContext.sendBroadcast(intent);
            }
            catch (Exception e)
            {
            }
        }
        else
        {
            Intent intent = new Intent();
            intent.setAction("WM_AMLINK_WND_HIDE");
            try
            {
                mContext.sendBroadcast(intent);
            }
            catch (Exception e)
            {
            }
        }
    }

    private static final String[] NTP_Server = {"time.windows.com", "time.nist.gov", "time-nw.nist.gov", "time-a.nist.gov", "time-b.nist.gov"};

    private boolean getAutoTime()
    {
        try
        {
            return Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AUTO_TIME) > 0;
        }
        catch (Exception snfe)
        {
            return false;
        }
    }

    private void revertToNitz()
    {
        if (!getAutoTime())
        {
            return;
        }
        //        int tryCount = 3;
        //        SntpClient client = new SntpClient();
        //
        //        WLog.i(TAG, "revertToNitz new sntpclient");
        //
        //        for (int count = 0; count < NTP_Server.length; count++)
        //        {
        //            for (int i = 0; i < tryCount; i++)
        //            {
        //                if (!getAutoTime())
        //                {
        //                    return;
        //                }
        //                if (client.requestTime(NTP_Server[count], 10000))
        //                {
        //                    long cachedNtp = client.getNtpTime();
        //                    long cachedNtpTimestamp = SystemClock.elapsedRealtime();
        //
        //                    WLog.i(TAG, "Sntp NtpTime = " + cachedNtp);
        //
        //                    setAndBroadcastNetworkSetTime(
        //                            cachedNtp + (SystemClock.elapsedRealtime() - client.getNtpTimeReference()));
        //                    return;
        //                }
        //            }
        //        }
    }

    private void setAndBroadcastNetworkSetTime(long time)
    {
        if (!getAutoTime())
        {
            return;
        }
        SystemClock.setCurrentTimeMillis(time);
        Intent intent = new Intent("android.intent.action.NETWORK_SET_TIME");
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra("time", time);
        try
        {
            mContext.sendStickyBroadcast(intent);
        }
        catch (Exception e)
        {
        }
    }

    private void updateTimeBySntp()
    {
        sWorker.post(new Runnable()
        {
            @Override
            public void run()
            {
                revertToNitz();
            }
        });
    }

    private ContentObserver mAutoTimeObserver = new ContentObserver(new Handler())
    {
        @Override
        public void onChange(boolean selfChange)
        {
            WLog.i(TAG, "Jon: Sntp Auto time state called ...,selfChange is:" + selfChange);
            updateTimeBySntp();
        }
    };


    private ContentObserver mPhoneBookSyncObserver = new ContentObserver(new Handler())
    {
        @Override
        public void onChange(boolean selfChange)
        {
            if (mCurrenAddr != null && mCurrenAddr.length() > 0)
            {
                BtDeviceRemote btDevice;
                synchronized (mRemoteDeviceMap)
                {
                    btDevice = mRemoteDeviceMap.get(mCurrenAddr);
                }
                if (btDevice != null)
                {
                    btDevice.onPBSycSettingsChanged();
                }
            }
        }
    };


    boolean isCustomBtSwitchOn()
    {
        return (Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.BLUETOOTH_ON, 0) == 1);
    }


    //	private ContentObserver mBtEnableObserver = new ContentObserver(
    //			new Handler()) {
    //		@Override
    //		public void onChange(boolean selfChange) {
    //           new Thread() {
    //			    @Override
    //			    public void run() {
    //			        WLog.d(TAG,"JonDEBUG: mBtEnableObserver Enter");
    //                    if(isCustomBtSwitchOn()) {
    //				        enable();
    //			        }else {
    //			            disable(true);
    //			        }
    //			    }
    //           	}.start();
    //		}
    //	};

    public void setCommandInterface(BtCommandInterface mbtCommandInterface)
    {
        commandInterface = mbtCommandInterface;
        commandInterface.setOnDeviceInquiry(mMainThreadHandler, EVENT_DEVICE_INQUIRY, null);
        commandInterface.setOnDeviceInquiryFinished(mMainThreadHandler, EVENT_DEVICE_INQUIRY_FINISHED, null);
        commandInterface.setOnParingResponse(mMainThreadHandler, EVENT_DEVICE_PAIRING_RESPONSE, null);
        commandInterface.setOnPasskeyResponse(mMainThreadHandler, EVENT_DEVICE_PASSKEY_RESPONSE, null);
        commandInterface.setOnDeleteTrustedIdResponse(mMainThreadHandler, EVENT_DEVICE_REMEOVE_BONDED, null);
        commandInterface.setOnPlayerMetadataChanged(mMainThreadHandler, EVENT_DEVICE_PLAYER_METADATA, null);
        commandInterface.setOnCallStatusChanged(mMainThreadHandler, EVENT_DEVICE_CALL_STATUS, null);
        //		commandInterface.setDeviceConnectStateChanged(mMainThreadHandler,
        //				EVENT_DEVICE_CONNECT_STATE_CHANGED, null);
        commandInterface.setBtEnableStateChanged(mMainThreadHandler, EVENT_BT_ENABLE_STATE_CHANGED, null);
        commandInterface.setOnPlayerStatusChanged(mMainThreadHandler, EVENT_DEVICE_PALYER_STATUS, null);
        commandInterface.setBtPowerReady(mMainThreadHandler, EVENT_BT_POWER_READY, null);
        commandInterface.setBtSourceStatusChanged(mMainThreadHandler, EVENT_BT_PLAYER_SOURCE_STATUS, null);
        commandInterface.setBtHostAddressChanged(mMainThreadHandler, EVENT_BT_HOST_ADDRES, null);
        /////////////////////////////////////////////////////////
        commandInterface.setHFPConnStateChanged(mMainThreadHandler, EVENT_BT_HFP_CONNECT_STATE_CHANGED, null);
        commandInterface.setHFPSvcStateChanged(mMainThreadHandler, EVENT_BT_HFP_SERVICE_STATE_CHANGED, null);
        commandInterface.setA2DPConnStateChanged(mMainThreadHandler, EVENT_BT_A2DP_CONNECT_STATE_CHANGED, null);
        commandInterface.setAVRCPConnStateChanged(mMainThreadHandler, EVENT_BT_AVRCP_CONNECT_STATE_CHANGED, null);

        commandInterface.setA2DPSvcStateChanged(mMainThreadHandler, EVENT_BT_A2DP_SERVICE_STATE_CHANGED, null);
        commandInterface.setPBAPConnStateChanged(mMainThreadHandler, EVENT_BT_PBAP_CONNECT_STATE_CHANGED, null);
        commandInterface.setPBAPSvcStateChanged(mMainThreadHandler, EVENT_BT_PBAP_SERVICE_STATE_CHANGED, null);
        commandInterface.setMAPConnStateChanged(mMainThreadHandler, EVENT_BT_MAP_CONNECT_STATE_CHANGED, null);

        commandInterface.setRemoteDevNameChanged(mMainThreadHandler, EVENT_BT_REMOTE_NAME_CHANGED, null);

        commandInterface.setLocalDevNameChanged(mMainThreadHandler, EVENT_BT_LOCAL_NAME_CHANGED, null);

        commandInterface.setPullPBCmtChanged(mMainThreadHandler, EVENT_BT_PULLPB_CMT_CHANGED, null);

        commandInterface.setGetMessageDataIndChanged(mMainThreadHandler, EVENT_BT_GETMSG_IND_CHANGED, null);

        commandInterface.setGetMessageCmtChanged(mMainThreadHandler, EVENT_BT_GETMSG_CMT_CHANGED, null);

        commandInterface.setPushMessageDataIndChanged(mMainThreadHandler, EVENT_BT_PUSHMSG_IND_CHANGED, null);

        commandInterface.setPushMessageCmtChanged(mMainThreadHandler, EVENT_BT_PUSHMSG_CMT_CHANGED, null);

        commandInterface.setHFAudioStatusChanged(mMainThreadHandler, EVENT_BT_HFAUDIO_STAUS_CHANGED, null);

        commandInterface.setMessageEvtChanged(mMainThreadHandler, EVENT_BT_MSGEVT_STAUS_CHANGED, null);

        commandInterface.setMUTEStatusChanged(mMainThreadHandler, EVENT_BT_MUTE_STATUS_CHANGED, null);

        commandInterface.setBtPairedListChanged(mMainThreadHandler, EVENT_BT_PAIRED_LIST, null);

        commandInterface.setBtVGSIChanged(mMainThreadHandler, EVENT_BT_VOLUME_GAIN_CHANGED, null);

        commandInterface.setBtPhoneBookSzChanged(mMainThreadHandler, EVENT_BT_PHONEBOOK_SIZE_CHANGED, null);

        commandInterface.setRingStatusChanged(mMainThreadHandler, EVENT_BT_RING_STATUS_CHANGED, null);

        commandInterface.setAVRCPPLAYCmdStatusChanged(mMainThreadHandler, EVENT_BT_AVRCP_PLAYCMD_STATE_CHANGED, null);

        commandInterface.setAVRCPPAUSECmdStatusChanged(mMainThreadHandler, EVENT_BT_AVRCP_PAUSECMD_STATE_CHANGED, null);
        /////////////////////////////////////////////////////////

    }

    public BtManagerService(Context context)
    {
        sMe = this;
        //        android.os.ServiceManager.addService("chbt", this);
        mBtMainThread = new HandlerThread("BtMainThread");
        mBtMainThread.start();
        mContext = context;
        //		mAddrDeviceMap = new TreeMap<String, BtDevice>();
        mRemoteDeviceMap = new TreeMap<String, BtDeviceRemote>();
        mMainThreadHandler = new MainThreadHandler(mBtMainThread.getLooper());

        mMediaInfo = new MediaInfo();
        mBtHost = new BtHost();

        // mUserSource = new UserInputSource(UserInputSource.TYPE_FRONT,
        // UserInputSource.ID_BLUETOOTH_MUSIC);


        // observer auto time
        sWorkerThread = new HandlerThread("btservice-thread");
        sWorkerThread.start();
        sWorker = new Handler(sWorkerThread.getLooper());
        //mContext.getContentResolver().registerContentObserver(
        //		Settings.Global.getUriFor(Settings.Global.AUTO_TIME), true,
        //		mAutoTimeObserver);
        //	mContext.getContentResolver().registerContentObserver(
        //			Settings.System.getUriFor(Settings.System.BLUETOOTH_ON), true,
        //			mBtEnableObserver);

        //        mContext.getContentResolver()
        //                .registerContentObserver(Settings.System.getUriFor(Settings.System.BLUETOOTH_PHONEBOOK_SYNC), true,
        //                        mPhoneBookSyncObserver);

        // BT Check Power
        sBTCheckSysPowerThread = new HandlerThread("btCheckSysPower-thread");
        sBTCheckSysPowerThread.start();
        sBTCheckSysPower = new Handler(sBTCheckSysPowerThread.getLooper());

        // BT Check Enable
        sBTCheckEnableStatusThread = new HandlerThread("btCheckEnable-thread");
        sBTCheckEnableStatusThread.start();
        sBTCheckEnableStatus = new Handler(sBTCheckEnableStatusThread.getLooper());

        mAutoConnRunnabler = new AutoConnectedRunnable();
        mAutoConnectThread = new Thread(mAutoConnRunnabler, "BTAudoConnect");
        mAutoConnectThread.start();

        //Music Handler
        mMusicThread = new HandlerThread("BtMusicThread");
        mMusicThread.start();
        mMusicHandler = new MusicHandler(mMusicThread.getLooper());

    }


    class AutoConnectedRunnable implements Runnable
    {
        AutoConnectedRunnable()
        {
        }

        public void run()
        {
            for (; ; )
            {
                if (getAutoConnMode() == 1)
                {
                    try
                    {
                        if (getBtHostState() == BluetoothManager.STATE_ON &&
                                mLastConnectedAddr != null &&
                                isAllDeviceDisconnected() &&
                                mBtHost != null &&
                                mBtHost.scanMode != BluetoothManager.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
                        {
                            setDeviceConnectedInternal(mLastConnectedAddr, true);
                        }
                    }
                    catch (Exception ex)
                    {
                    }
                }
                try
                {
                    Thread.sleep(30 * 1000);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }


    private void listenBTSource()
    {
        WLog.d(TAG, "JonPHONE: BT player: listenBTSource Enter,mIsBtSourceConnected is:" + mIsBtSourceConnected);
        try
        {
            //            mSourceListener.listen();
            //            mSourceListener.getSource();
            synchronized (mBTMusicSourceLock)
            {
                isListeningBTSource = true;
                WLog.d(TAG, "JonPHONE: BT player: listenBTSource ok");
            }
        }
        catch (Exception ex)
        {
            WLog.d(TAG, "JonPHONE: BT player: listenBTSource Exception:" + ex);
        }
    }

    private void exitBTSource()
    {
        WLog.d(TAG, "JonPHONE: BT player: exitBTSource Enter");
        try
        {
        }
        catch (Exception ex)
        {
            WLog.d(TAG, "JonPHONE: BT player: exitBTSource Exception:" + ex);
        }
    }

    void sleep(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
        }
    }

    boolean isInPhoneMode = false;

    void notifyUnMuteInPhoneMode(boolean mute)
    {
        Intent intent = new Intent("com.chleon.phone.autounmute.changed");
        intent.putExtra("mute", mute);
        try
        {
            mContext.sendBroadcast(intent);
        }
        catch (Exception e)
        {
        }
    }

    private void broadcastTtsSpeakStop()
    {
        Intent i = new Intent("com.chleon.action.speech.sound.stop");
        try
        {
            mContext.sendBroadcast(i);
        }
        catch (Exception e)
        {
        }
    }

    @Override
    public void sendCommand(String atCommand)
    {
        commandInterface.sendCommand(atCommand, null);
    }

    public void close()
    {
        commandInterface.close();
    }

    @Override
    public boolean isEnabled()
    {
        return isEnabledLocked();
    }

    @Override
    public boolean bluetooth_enable(boolean on)
    {
        WLog.d(TAG, "Jon: bluetooth_enable:" + on);
        if (on)
        {
            new Thread()
            {
                public void run()
                {
                    Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.BLUETOOTH_ON, 1);
                    enable();
                }
            }.start();
        }
        else
        {
            new Thread()
            {
                public void run()
                {
                    Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.BLUETOOTH_ON, 0);
                    disable(true);
                }
            }.start();
        }
        return true;
    }

    @Override
    public boolean enable()
    {
        int hostState = getBtHostState();
        WLog.d(TAG, "enable Enter,hostState:" + hostState);

        isBTStandby = false;
        if (hostState == BluetoothManager.STATE_ON || hostState == BluetoothManager.STATE_TURNING_ON)
        {
            return true;
        }
        WLog.d(TAG, "enable post runnable");
        setBtHostState(BluetoothManager.STATE_TURNING_ON);
        boolean ret = (Boolean) sendRequest(CMD_ENABLE, null);
        WLog.d(TAG, "enable result:" + ret);
        //setBtHostState(BluetoothManager.STATE_ON);
        return true;
    }

    boolean isEnabledLocked()
    {
        return getBtHostState() == BluetoothManager.STATE_ON;
    }

    @Override
    public boolean disable(boolean persistSetting)
    {
        int hostState = getBtHostState();
        WLog.d(TAG, "Jon: [BT]  disable Enter,hostState:" + hostState);
        if (!persistSetting)
        {
            isBTStandby = true;
        }
        else
        {
            isBTStandby = false;
        }

        if (hostState == BluetoothManager.STATE_OFF || hostState == BluetoothManager.STATE_TURNING_OFF)
        {
            return true;
        }
        WLog.d(TAG, "JonDEBUG: disable post runnable");
        setBtHostState(BluetoothManager.STATE_TURNING_OFF);
        boolean ret = (Boolean) sendRequest(CMD_DISABLE, null);
        WLog.d(TAG, "JonPHONE: disable result:" + ret);
        setBtHostState(BluetoothManager.STATE_OFF);
        return true;
    }

    @Override
    public int getBluetoothState()
    {
        int state = getBtHostState();
        if (DBG)
        {
            WLog.d(TAG, "Jon: getBluetoothState Enter,state:" + state);
        }
        return state;
    }

    @Override
    public String getSwVersion()
    {
        WLog.e(TAG, "JonPHONE: Enter, BT version is:" + mBTVersion);
        String version = System.getProperty("persist.sys.bt.version", "--");
        if (mBTVersion != null && mBTVersion.length() > 0 && !mBTVersion.equals("--"))
        {
            if (version.equals("--"))
            {
                System.setProperty("persist.sys.bt.version", mBTVersion);
            }
            return mBTVersion;
        }
        else
        {
            mBTVersion = (String) sendRequest(CMD_GET_SW_VERSION, null);
            WLog.e(TAG, "JonPHONE: get BT version is:" + mBTVersion);
            if ("--".equals(mBTVersion))
            {
                return version;
            }
            else
            {
                System.setProperty("persist.sys.bt.version", mBTVersion);
                return mBTVersion;
            }
        }
    }

    @Override
    public boolean enableUpdateMode()
    {
        return false;
    }

    @Override
    public String getName()
    {
        WLog.d(TAG, "getName Enter !!!!!!");

        if (mBtHostName != null && mBtHostName.length() > 0 && !mBtHostName.equals("UNKNOWN"))
        {
            WLog.d(TAG, "!!!!!! getName mBtHostName is :" + mBtHostName);
            return mBtHostName;
        }
        else
        {
            String device_name = Settings.System
                    .getString(mContext.getContentResolver(), BluetoothSettings.BLUETOOTH_DEVICE_NAME);
            if (device_name == null)
            {
                device_name = "";
            }
            sendRequest(CMD_GET_NAME, null);
            return device_name;
        }
    }

    @Override
    public boolean setName(String name)
    {
        boolean ret = false;
        WLog.d(TAG, "[BT] setName name:" + name);
        ret = (Boolean) sendRequest(CMD_SET_NAME, name);
        if (ret)
        {
            mBtHostName = name;
        }
        return ret;
    }

    @Override
    public String getAddress()
    {
        //	synchronized (mBtHost) {
        return mBtHost.address;
        //	}
    }

    @Override
    public int getScanMode()
    {
        //	synchronized (mBtHost) {
        return mBtHost.scanMode;
        //	}
    }

    @Override
    public boolean setScanMode(int mode, int duration)
    {
        //TODO for correct mode set.(SDK will redefine)
        if (mode == BluetoothManager.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
        {
            return (Boolean) sendRequest(CMD_START_DISCOVERABLE, null);
        }
        else
        {
            return (Boolean) sendRequest(CMD_STOP_DISCOVERABLE, null);
        }
    }

    boolean getTrustedList()
    {
        return (Boolean) sendRequest(CMD_TRUSTED_LIST, null);
    }

    @Override
    public boolean startDiscovery()
    {
        if (DBG)
        {
            WLog.d(TAG, "startDiscovery Enter");
        }
        if (isDiscoveringLocked())
        {
            return false;
        }
        boolean result = (Boolean) sendRequest(CMD_START_INQUIRY, null);
        return result;
    }

    @Override
    public boolean cancelDiscovery()
    {
        if (!isDiscoveringLocked())
        {
            return false;
        }
        return (Boolean) sendRequest(CMD_STOP_INQUIRY, null);
    }

    @Override
    public boolean isDiscovering()
    {
        return isDiscoveringLocked();
    }

    private boolean isDiscoveringLocked()
    {
        //synchronized (mBtHost) {
        return mBtHost.isInquiring;
        //}
    }

    @Override
    public String[] getBondedDevices()
    {
        ArrayList<String> addrArrList = new ArrayList<String>();
        synchronized (mRemoteDeviceMap)
        {
            Collection<BtDeviceRemote> devices = mRemoteDeviceMap.values();
            Iterator<BtDeviceRemote> iterator = devices.iterator();
            while (iterator.hasNext())
            {
                BtDeviceRemote btDevice = (BtDeviceRemote) iterator.next();
                addrArrList.add(btDevice.mAddress);
            }
        }
        return (String[]) addrArrList.toArray(new String[0]);
    }

    @Override
    public int getMaxBondedDeviceCount()
    {
        return max_pair_count;
    }

    @Override
    public boolean createBond(String address)
    {
        sendRequest(CMD_INITIATE_PAIRING, address);
        return true;
    }

    @Override
    public boolean cancelBondProcess(String address)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeBond(final String address)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                BtDeviceRemote mBtdevice = null;
                synchronized (mRemoteDeviceMap)
                {
                    mBtdevice = mRemoteDeviceMap.get(address);
                }
                if (mBtdevice != null)
                {
                    if (address != null && address.equals(mCurrenAddr))
                    {
                        try
                        {
                            setDeviceConnected(address, false);
                        }
                        catch (Exception ex)
                        {
                        }
                    }
                    sendRequest(CMD_DELETE_TRUSTED, address);
                    removeRemoteDeviceList(mBtdevice.mAddress);
                }
            }
        }.start();
        return true;
    }

    //no Used,if remove it?
    public boolean getTrustState(String address)
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public String getRemoteName(String address)
    {
        String name = null;
        BtDeviceRemote device;
        synchronized (mRemoteDeviceMap)
        {
            device = mRemoteDeviceMap.get(address);
        }
        if (device != null)
        {
            name = device.getName();
        }
        if (name != null)
        {
            return name;
        }
        else
        {
            return address;
        }
    }

    public int getPhonebookSize(String address)
    {
        return -1;
    }

    @Override
    public ParcelUuid[] getRemoteUuids(String address)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean fetchRemoteUuids(String address, ParcelUuid uuid) throws RemoteException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int addRfcommServiceRecord(String serviceName, ParcelUuid uuid, int channel, IBinder b)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void removeServiceRecord(int handle)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public int getBondState(String address)
    {
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                return btDevice.getBondState();
            }
        }
        return BluetoothDevice.BOND_NONE;
    }

    @Override
    public boolean setPin(String address, byte[] pin)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setPairingConfirmation(String address, boolean confirm) throws RemoteException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean cancelPairingUserInput(String address) throws RemoteException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setTrust(String address, boolean value) throws RemoteException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void call(String number)
    {
        if (number == null || number.length() == 0)
        {
            return;
        }
        sendRequest(CMD_INITIATE_CALL, number);
    }

    private boolean isNumeric(String str)
    {
        for (int i = str.length(); --i >= 0; )
        {
            if (!Character.isDigit(str.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    public String getVoiceReportCallId(String text)
    {
        String callid = text;
        if (isNumeric(text))
        {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < text.length(); i++)
            {
                buf.append(text.charAt(i));
                buf.append(' ');
            }
            callid = buf.toString();
        }
        return callid;
    }

    private String getContactNameByNumber(String number)
    {
        String name = "";
        try
        {
            Cursor cursor = mContext.getContentResolver()
                    .query(Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, number),
                            new String[]{PhoneLookup._ID, PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME, PhoneLookup.TYPE, PhoneLookup.LABEL},
                            null, null, null);

            while (cursor != null && cursor.moveToNext())
            {
                int numberFieldColumnIndex = cursor.getColumnIndex(PhoneLookup.NUMBER);
                String num = cursor.getString(numberFieldColumnIndex);
                if (number != null && number.replace(" ", "").equals(num))
                {
                    int nameFieldColumnIndex = cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME);
                    name = cursor.getString(nameFieldColumnIndex);
                    break;
                }
            }
            if (cursor != null)
            {
                cursor.close();
            }
        }
        catch (Exception ex)
        {
        }
        if (name != null && name.length() > 0)
        {
            return name;
        }
        return number;
    }

    @Override
    public void callLastNumber()
    {
        // switchPhoneSource(true);
        sendRequestAsync(CMD_INITIATE_LAST_CALL);
    }

    @Override
    public void recieveIncomingCall()
    {
        // switchPhoneSource(true);
        if (mCallStatusArray != null && mCallStatusArray.size() > 1)
        {
            CallStatus call = getFirstActiveRingingCall();
            if (call != null)
            // sendRequest(CMD_MULTICALL_ACTION, BTConstants.BT_ACTION_MULTICALL_UDUB);
            {
                sendCommand("AT+B HFMCAL 2,0");
            }
        }
        else
        {
            sendRequestAsync(CMD_RECIEVE_INCOMING_CALL);
        }
    }

    ArrayList<CallStatus> callState()
    {
        return (ArrayList<CallStatus>) sendRequest(CMD_CALL_STATE, null);
    }

    @Override
    public int getCallState()
    {
        int callState = BTConstants.BT_CALL_STATE_FREE;
        return getCurrentCallState();
    }

    public String getCallDetail()
    {
        StringBuffer ret = new StringBuffer();
        synchronized (mCallStatusArray)
        {
            if (mCallStatusArray == null || mCallStatusArray.size() == 0)
            {
                return null;
            }
            for (int i = 0; i < mCallStatusArray.size(); i++)
            {
                CallStatus status = mCallStatusArray.get(i);
                ret.append("INDEX:");
                ret.append(status.callIndex);
                ret.append(";");
                ret.append("CALLERID:");
                ret.append(status.callerId);
                ret.append(";");
                ret.append("STATE:");
                ret.append(status.callState);
                ret.append(";");
                ret.append("ACTIVEDTIME:");
                ret.append(status.activeStartTime);
                ret.append(";");
                ret.append("\n");
            }
        }
        return ret.toString();
    }

    @Override
    public int getNetworkType()
    {
        // TODO to implement network type
        return 0;
    }

    @Override
    public int getSignalStrength()
    {
        return 0;
    }

    int PLAYER_STATE_STARTING = 1;
    int PLAYER_STATE_PLAY = 2;
    int PLAYER_STATE_PAUSING = 3;
    int PLAYER_STATE_PAUSE = 4;

    int mPlayerState = PLAYER_STATE_PAUSE;

    private final int ACTION_PLAYER_START = 0;
    private final int ACTION_PLAYER_RESUME = 1;
    private final int ACTION_PLAYER_PAUSE = 2;
    private final int ACTION_PLAYER_STOP = 3;
    private final int ACTION_PLAYER_FORWARD = 4;
    private final int ACTION_PLAYER_BACKWARD = 5;

    class MusicHandler extends Handler
    {
        public MusicHandler(Looper loop)
        {
            super(loop);
        }

        @Override
        public void handleMessage(Message message)
        {
            switch (message.what)
            {
                case ACTION_PLAYER_START:
                {
                    WLog.d(TAG, "JonMusic: ACTION_PLAYER_START +++++++");

                    if (/*!isPlaying()*/mPlayerState == PLAYER_STATE_PAUSING || mPlayerState == PLAYER_STATE_PAUSE)
                    {
                        musicResume();
                    }
                    break;
                }
                case ACTION_PLAYER_RESUME:
                {
                    WLog.d(TAG, "JonMusic: ACTION_PLAYER_RESUME");
                    setPlayerState(PLAYER_STATE_STARTING);
                    sendRequest(CMD_PLAYER_ACTION, BTConstants.BT_ACTION_PLAYER_RESUME);
                    break;
                }
                case ACTION_PLAYER_STOP:
                {
                    WLog.d(TAG, "JonMusic: ACTION_PLAYER_STOP -------");
                    if (mPlayerState == PLAYER_STATE_PLAY || mPlayerState == PLAYER_STATE_STARTING)
                    {
                        setPlayerState(PLAYER_STATE_PAUSING);
                        sendRequest(CMD_PLAYER_ACTION, BTConstants.BT_ACTION_PLAYER_PAUSE);
                        exitBTSource();
                    }
                    break;
                }
                case ACTION_PLAYER_PAUSE:
                {
                    WLog.d(TAG, "JonMusic: ACTION_PLAYER_PAUSE");
                    setPlayerState(PLAYER_STATE_PAUSING);
                    sendRequest(CMD_PLAYER_ACTION, BTConstants.BT_ACTION_PLAYER_PAUSE);
                    break;
                }
                case ACTION_PLAYER_FORWARD:
                {
                    WLog.d(TAG, "JonMusic: ACTION_PLAYER_FORWARD");
                    sendRequest(CMD_PLAYER_ACTION, BTConstants.BT_ACTION_PLAYER_NEXT);
                    break;
                }
                case ACTION_PLAYER_BACKWARD:
                {
                    WLog.d(TAG, "JonMusic: ACTION_PLAYER_BACKWARD");
                    sendRequest(CMD_PLAYER_ACTION, BTConstants.BT_ACTION_PLAYER_PREVIOUS);
                    break;
                }
            }
        }
    }

    void removeAllMusicActions()
    {
        mMusicHandler.removeMessages(ACTION_PLAYER_START);
        mMusicHandler.removeMessages(ACTION_PLAYER_RESUME);
        mMusicHandler.removeMessages(ACTION_PLAYER_STOP);
        mMusicHandler.removeMessages(ACTION_PLAYER_PAUSE);
        mMusicHandler.removeMessages(ACTION_PLAYER_FORWARD);
        mMusicHandler.removeMessages(ACTION_PLAYER_BACKWARD);
    }

    void setPlayerState(int state)
    {
        mPlayerState = state;
        WLog.d(TAG, "JonMusicState: " + state);
    }

    @Override
    public void musicResume()
    {
        WLog.d(TAG, "JonMusic: resume Enter");
        synchronized (mMusicHandler)
        {
            removeAllMusicActions();
            mMusicHandler.sendEmptyMessage(ACTION_PLAYER_RESUME);
        }
    }

    @Override
    public void musicStop()
    {
        WLog.d(TAG, "JonMusic: stop Enter");
        synchronized (mMusicHandler)
        {
            removeAllMusicActions();
            mMusicHandler.sendEmptyMessage(ACTION_PLAYER_PAUSE);
        }
    }

    @Override
    public void musicPause()
    {
        WLog.d(TAG, "JonMusic: pause Enter");
        synchronized (mMusicHandler)
        {
            removeAllMusicActions();
            mMusicHandler.sendEmptyMessage(ACTION_PLAYER_PAUSE);
        }

    }

    @Override
    public void playNextTrack()
    {
        WLog.d(TAG, "JonMusic: playNextTrack Enter");
        synchronized (mMusicHandler)
        {
            removeAllMusicActions();
            mMusicHandler.sendEmptyMessage(ACTION_PLAYER_FORWARD);
        }
    }

    @Override
    public void playPreviousTrack()
    {
        WLog.d(TAG, "JonMusic: playPreviousTrack Enter");
        synchronized (mMusicHandler)
        {
            removeAllMusicActions();
            mMusicHandler.sendEmptyMessage(ACTION_PLAYER_BACKWARD);
        }
    }


    @Override
    public boolean playerStart(final String address)
    {
        WLog.d(TAG, "JonMusic: playerStart Enter JonMusicState is:" + mPlayerState);
        listenBTSource();
        synchronized (mMusicHandler)
        {
            removeAllMusicActions();
            mMusicHandler.sendEmptyMessageDelayed(ACTION_PLAYER_START, 300);
        }
        return true;
    }

    public boolean playerStop()
    {
        WLog.d(TAG, "JonMusic: playerStop Enter");
        synchronized (mMusicHandler)
        {
            removeAllMusicActions();
            mMusicHandler.sendEmptyMessageDelayed(ACTION_PLAYER_STOP, 500);
        }
        return true;
    }

    @Override
    public int getCurrentPosition()
    {
        getPlayerStatus();
        return mMediaInfo.position * 1000;
    }

    @Override
    public int getDuration()
    {
        getPlayerStatus();
        return mMediaInfo.duration * 1000;
    }

    @Override
    public boolean isPlaying()
    {
        if (DBG)
        {
            WLog.d(TAG, "BT player: isPlaying Enter");
        }
        if (mMediaInfo.getPlayActivity() == BluetoothDevice.PLAYER_STATUS_PLAY)
        {
            return true;
        }
        return false;
    }

    String[] getPlayerStatus()
    {
        return (String[]) sendRequest(CMD_PLAYER_STATUS, null);
    }

    private void updateMediaInfo(Bundle b)
    {
        synchronized (mMediaInfo)
        {
            int metaDataMask = b.getInt("mask");
            switch (metaDataMask)
            {
                case MediaInfo.MEDIA_METADATA_TITTLE:
                    mMediaInfo.title = b.getString("title");
                    break;
                case MediaInfo.MEDIA_METADATA_ARTIST:
                    mMediaInfo.artist = b.getString("artist");
                    break;
            }
        }
        notifyPlayerMetaDataChanged();
        notifyMessageToMenu(true);
    }

    @Override
    public boolean setMuteState(boolean state)
    {
        return (Boolean) sendRequest(CMD_PLAYER_MUTE_STATE, state);
    }

    @Override
    public String getArtist()
    {
        if (mMediaInfo.artist == null || mMediaInfo.artist.length() == 0)
        {
            //	return mContext.getString(com.android.internal.R.string.unknown_artist);
            return "Unknown";
        }
        else
        {
            return mMediaInfo.artist;
        }
    }

    @Override
    public String getAlbum()
    {
        return mMediaInfo.album;
    }

    @Override
    public int getTrack()
    {
        return mMediaInfo.track;
    }

    @Override
    public String getTittle()
    {
        if (mMediaInfo.title == null || mMediaInfo.title.length() == 0)
        {
            return "";
        }
        else
        {
            return mMediaInfo.title;
        }
    }

    @Override
    public String getComposer()
    {
        return mMediaInfo.composer;
    }

    @Override
    public String getGenre()
    {
        return mMediaInfo.genre;
    }

    @Override
    public boolean setRepeateMode(int mode)
    {
        boolean status = false;
        //		if (mode == 0x02) {
        //			status = (Boolean) sendRequest(CMD_PALYER_SET_REPEATE_MODE,
        //					BTConstants.PALYER_MODE_REPEATE_CURRENT);
        //		} else if (mode == 0x03) {
        //			status = (Boolean) sendRequest(CMD_PALYER_SET_REPEATE_MODE,
        //					BTConstants.PALYER_MODE_REPEATE_ALL);
        //		}
        return status;
    }

    String repeateMode()
    {
        return (String) sendRequest(CMD_PALYER_GET_REPEATE_MODE, null);
    }

    public int getRepeateMode()
    {
        String repeatStr = repeateMode();
        int mRepeateMode = 0;
        // if(repeatStr!=null){
        // mRepeateMode=ApuBtUtils.convertstateToRepeateMode(Integer.parseInt(repeatStr));
        // }
        return mRepeateMode;
    }

    public boolean hangUpCall()
    {
        if (DBG)
        {
            WLog.d(TAG, "Jon: hangUpCall Enter");
        }
        int mcallCount = 0;
        int mcallIndex = 0;
        // ArrayList<CallStatus> callStateArray =callState();
        if (mCallStatusArray == null)
        {
            return false;
        }
        if (mCallStatusArray.size() > 1)
        {
            CallStatus call = getFirstActiveRingingCall();
            if (call != null)
            {
                //return (Boolean) sendRequest(CMD_MULTICALL_ACTION, BTConstants.BT_ACTION_MULTICALL_UDUB);
                sendCommand("AT+B HFMCAL 0,0");
                return true;
            }
            else
            {
                //return (Boolean) sendRequest(CMD_MULTICALL_ACTION, BTConstants.BT_ACTION_MULTICALL_RAAH);
                sendCommand("AT+B HFMCAL 1,0");
                return true;
            }
        }
        else
        {
            Object[] data = new Object[2];
            data[0] = mcallIndex;
            data[1] = mcallCount;
            return (Boolean) sendRequest(CMD_CALL_HANG_UP, data);
        }
    }

    @Override
    public boolean rejectCall()
    {
        return hangUpCall();
    }

    @Override
    public void disconnectCall()
    {
        int tryTimes = 3;
        boolean ret = false;
        while (tryTimes > 0 && !ret)
        {
            ret = hangUpCall();
            if (!ret)
            {
                sleep(1000);
            }
            tryTimes--;
        }
    }

    @Override
    public boolean muteCall(boolean state)
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSupportPhoneBook(String address)
    {
        return true;
    }

    @Override
    public boolean isPhoneBookSynced(String address)
    {
        return false;
    }

    @Override
    public int getPhoneBookSyncedStatus(String address)
    {
        if (DBG)
        {
            WLog.d(TAG, "Jon getPhoneBookSyncedStatus Enter");
        }
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                return btDevice.getPhoneBookSyncedStatus();
            }
        }
        return -2;
    }

    @Override
    public String getConnectDeviceAddr(int profile)
    {
        WLog.d(TAG, "Jon getConnectDeviceAddr Enter");
        synchronized (mRemoteDeviceMap)
        {
            Collection<BtDeviceRemote> devices = mRemoteDeviceMap.values();
            Iterator<BtDeviceRemote> iterator = devices.iterator();
            while (iterator.hasNext())
            {
                BtDeviceRemote btDevice = (BtDeviceRemote) iterator.next();
                switch (profile)
                {
                    case BtDeviceRemote.SVC_PHONE:
                        if (btDevice.getHFPSvcState() == BtDeviceRemote.SVC_CONNECTED)
                        {
                            return btDevice.mAddress;
                        }
                        break;
                    case BtDeviceRemote.SVC_A2DP_SNK:
                        if (btDevice.getA2DPSvcState() == BtDeviceRemote.SVC_CONNECTED)
                        {
                            return btDevice.mAddress;
                        }
                        break;
                    case BtDeviceRemote.SVC_MAP:
                        if (btDevice.getMAPSvcState() == BtDeviceRemote.MAP_CON_STATE_SVC_SUCCESS)
                        {
                            return btDevice.mAddress;
                        }
                        break;
                    default:
                        if (btDevice.getState() == BluetoothDevice.CONNECT_CONNECTED)
                        {
                            return btDevice.mAddress;
                        }
                        break;
                }
            }
        }
        return null;
    }


    @Override
    public String GetConnectDeviceAddr(int profile)
    {
        WLog.d(TAG, "Jon GetConnectDeviceAddr Enter");
        synchronized (mRemoteDeviceMap)
        {
            Collection<BtDeviceRemote> devices = mRemoteDeviceMap.values();
            Iterator<BtDeviceRemote> iterator = devices.iterator();
            while (iterator.hasNext())
            {
                BtDeviceRemote btDevice = (BtDeviceRemote) iterator.next();
                switch (profile)
                {
                    case BtDeviceRemote.SVC_PHONE:
                        if (btDevice.getHFPSvcState() == BtDeviceRemote.SVC_CONNECTED)
                        {
                            return btDevice.mAddress;
                        }
                        break;
                    case BtDeviceRemote.SVC_A2DP_SNK:
                        if (btDevice.getA2DPSvcState() == BtDeviceRemote.SVC_CONNECTED)
                        {
                            return btDevice.mAddress;
                        }
                        break;
                    case BtDeviceRemote.SVC_MAP:
                        if (btDevice.getMAPSvcState() == BtDeviceRemote.MAP_CON_STATE_SVC_SUCCESS)
                        {
                            return btDevice.mAddress;
                        }
                        break;
                    default:
                        if (btDevice.getState() == BluetoothDevice.CONNECT_CONNECTED)
                        {
                            return btDevice.mAddress;
                        }
                        break;
                }
            }
        }
        return null;
    }

    @Override
    public int getRssi(final String address)
    {
        WLog.d(TAG, "Jon: getRssi Enter");
        return -128;
    }


    @Override
    public void getPhoneBookByManual(String address)
    {
        if (DBG)
        {
            WLog.d(TAG, "Jon getPhoneBookByManual Enter");
        }
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                btDevice.getPhoneBookByManual();
            }
        }
    }


    @Override
    public int getPhoneBookSyncProgress(String address)
    {
        if (DBG)
        {
            WLog.d(TAG, "Jon getPhoneBookSyncProgress Enter");
        }
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                return btDevice.getPhoneBookSyncProgress();
            }
        }
        return 0;
    }

    @Override
    public int getState(String address)
    {
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                return btDevice.getState();
            }
        }
        return BluetoothDevice.CONNECT_DISCONNECTED;
    }

    @Override
    public int getPhonePrivateMode()
    {
        return mPhoneHFAudioConnected;
    }

    @Override
    public boolean setPhonePrivateMode(int mode)
    {
        return (Boolean) sendRequest(CMD_SET_DISCRET_MODE, mode);
    }

    @Override
    public int getAutoConnMode()
    {
        // int mode = (Integer) sendRequest(CMD_GET_AUTOCONN_MODE, null);
        // WLog.d(TAG,"Jon: getAutoConnMode: "+mode);
        int mode = Settings.System
                .getInt(mContext.getContentResolver(), BluetoothSettings.BLUETOOTH_AUTO_CONNECTION, 1);
        return mode;
    }

    @Override
    public boolean setAutoConnMode(int mode)
    {
        Settings.System.putInt(mContext.getContentResolver(), BluetoothSettings.BLUETOOTH_AUTO_CONNECTION, mode);
        return (Boolean) sendRequest(CMD_SET_AUTOCONN_MODE, mode);
    }

    @Override
    public int getMicMuteState()
    {
        return mMicUnMuteState;//0: Muted 1: unMuted
    }

    @Override
    public boolean setMicMuteState(int unMuted)
    {
        mMicUnMuteState = unMuted;
        return (Boolean) sendRequest(CMD_SET_MIC_MUTE_STATE, unMuted);
    }

    @Override
    public boolean generateDTMF(char value)
    {
        return (Boolean) sendRequest(CMD_GENERATE_DTMF, value);
    }

    @Override
    public boolean switchCalls()
    {
        return (Boolean) sendRequest(CMD_SWITCH_CALLS, null);
    }

    @Override
    public boolean setStartPBSyncManual(String address)
    {
        getPhoneBookByManual(address);
        return true;
    }

    // @Override
    // public boolean setEnableMultiSync(int enable) {
    // return (Boolean) sendRequest(CMD_GET_PB_MULTI_SYNC, enable);
    // }

    @Override
    public boolean setADCConfiguration(int type, int value)
    {
        int[] config = {type, value};
        return (Boolean) sendRequest(CMD_SET_AADC, config);
    }

    @Override
    public boolean setAudioVolume(int type, int value)
    {
        int[] btVolume = {type, value};
        return (Boolean) sendRequest(CMD_SET_VOLUME, btVolume);
    }

    @Override
    public int getAudioVolume(int type)
    {
        int value = (Integer) sendRequest(CMD_GET_VOLUME, type);
        // WLog.d(TAG,"Jon: getAudioVolume: "+value);
        return value;
    }

    @Override
    public int[] getAudioVolumeRange(int type)
    {
        int[] value = (int[]) sendRequest(CMD_GET_VOLUME_RANGE, type);
        // WLog.d(TAG,"Jon: getAudioVolumeRange: "+value);
        return value;
    }

    @Override
    public List<BluetoothMessage> getBtMessage(String address)
    {
        if (DBG)
        {
            WLog.d(TAG, "Jon getBtMessage Enter");
        }
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                return btDevice.getMessages();
            }
        }
        return null;
    }

    @Override
    public int getMapSvcState(String address)
    {
        if (DBG)
        {
            WLog.d(TAG, "Jon getMapSvcState Enter");
        }
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                return btDevice.getMAPSvcState();
            }
        }
        return -1;
    }

    @Override
    public int getPhoneSvcState(String address)
    {
        if (DBG)
        {
            WLog.d(TAG, "Jon getPhoneSvcState Enter");
        }
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                return btDevice.getHFPSvcState();
            }
        }
        return 0;
    }

    @Override
    public int getA2DPSinkSvcState(String address)
    {
        if (DBG)
        {
            WLog.d(TAG, "Jon getA2DPSinkSvcState Enter");
        }
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                return btDevice.getA2DPSvcState();
            }
        }
        return 0;
    }


    @Override
    public boolean isMapMsgDownloading(String address)
    {
        //		if (DBG)
        //			WLog.d(TAG, "Jon isMapMsgDownloading Enter");
        //		synchronized (mAddrDeviceMap) {
        //			BtDevice btDevice = mAddrDeviceMap.get(address);
        //			if (btDevice != null) {
        //				return btDevice.isMapMsgDownloading();
        //			}
        //		}
        return false;
    }

    public boolean retriveMapMessage(String address, int accountId, int msgId)
    {
        //if (DBG)
        WLog.d(TAG, "Jon retriveMapMessage Enter");
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                return btDevice.retriveMapMessage(accountId, msgId);
            }
        }
        return false;
    }

    public boolean sendMapMessage(String address, BluetoothMessage msg)
    {
        //		if (DBG)
        WLog.d(TAG, "Jon sendMapMessage Enter");
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                return btDevice.sendMapMessage(msg);
            }
        }
        return false;
    }


    @Override
    public boolean setDeviceConnected(String address, boolean state) throws RemoteException
    {
        WLog.d(TAG, "Jon: setDeviceConnected Enter,address is:" + address + ",state is:" + state);
        if (address != null && address.equals(mLastConnectedAddr))
        {
            mLastConnectedAddr = null;
        }
        //		WLog.d(TAG,"Jon: AutoConnectedRunnable setDeviceConnected mLastConnectedAddr is:"+mLastConnectedAddr);
        BtDeviceRemote device;
        synchronized (mRemoteDeviceMap)
        {
            device = mRemoteDeviceMap.get(address);
        }
        if (device != null)
        {
            return device.connect(state);
        }
        return false;
    }


    private boolean setDeviceConnectedInternal(String address, boolean state) throws RemoteException
    {
        WLog.d(TAG,
                "Jon: AutoConnectedRunnable setDeviceConnectedInternal Enter,address is:" + address + ",state is:" + state);
        BtDeviceRemote device;
        synchronized (mRemoteDeviceMap)
        {
            device = mRemoteDeviceMap.get(address);
        }
        if (device != null)
        {
            return device.connect(state);
        }
        return false;
    }


    public int getRemoteClass(String address)
    {
        return 0;
    }

    private boolean setProfileSupported(int mask)
    {
        return (Boolean) sendRequest(CMD_SET_PROFILE_MSK, mask);
    }

    public void dumpDevice()
    {
        //		synchronized (mAddrDeviceMap) {
        //			Collection<BtDevice> devices = mAddrDeviceMap.values();
        //			if (devices == null || devices.size() == 0) {
        //				if (DBG)
        //					WLog.d(TAG, "Jon: No BtDevice added");
        //			} else {
        //				Iterator<BtDevice> iterator = devices.iterator();
        //				while (iterator.hasNext()) {
        //					BtDevice btDevice = iterator.next();
        //					btDevice.dump();
        //				}
        //			}
        //		}
    }

    boolean isAllDeviceDisconnected()
    {
        synchronized (mRemoteDeviceMap)
        {
            Collection<BtDeviceRemote> devices = mRemoteDeviceMap.values();
            if (devices != null && devices.size() > 0)
            {
                Iterator<BtDeviceRemote> iterator = devices.iterator();
                while (iterator.hasNext())
                {
                    BtDeviceRemote btDevice = iterator.next();
                    if (btDevice.getState() != BluetoothDevice.CONNECT_DISCONNECTED)
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args)
    {
        //		synchronized (mBtHost) {
        //			mBtHost.dump(fd, pw);
        //			Collection<BtDevice> devices = mAddrDeviceMap.values();
        //			if (devices == null || devices.size() == 0) {
        //				pw.println("No BtDevice added");
        //			} else {
        //				Iterator<BtDevice> iterator = devices.iterator();
        //				while (iterator.hasNext()) {
        //					BtDevice btDevice = iterator.next();
        //					btDevice.dump(fd, pw);
        //				}
        //			}
        //			mMediaInfo.dump(fd, pw);
        //			pw.println("  Current MediaPlayState: "
        //					+ mMediaInfo.getPlayActivity());
        //		}
    }

    @Override
    public void ttsSpeak(String text)
    {
        if (DBG)
        {
            WLog.d(TAG, "Jon: tts ttsSpeak Enter");
        }
        TextToSpeech tts = null;
        if (tts != null)
        {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
        }
    }

    public static int MEDIA_STATUS_FM = 0;
    public static int MEDIA_STATUS_AUX = 1;
    public static int MEDIA_STATUS_BT = 2;
    public static int MEDIA_STATUS_CMMC = 3;
    public static int MEDIA_STATUS_DISC = 4;
    public static int MEDIA_STATUS_HDD = 5;
    public static int MEDIA_STATUS_USB = 6;

    public static int MEDIA_STATUS_NONE = -1;
    public static int MEDIA_STATUS_PLAY = 1;
    public static int MEDIA_STATUS_PAUSE = 2;


    public void notifyMessageToMenu(boolean updateTitle)
    {
    }

    public void notifyDeviceListChanged()
    {
        try
        {
            Intent intent = new Intent("com.chleon.action.btdevice.list.changed");
            sendBroadcast(intent);
        }
        catch (Exception e)
        {
        }
    }

    private void sendBroadcast(Intent intent)
    {
        //        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        mContext.sendBroadcast(intent);
    }

    private void addRemoteDeviceList(BtDeviceRemote device)
    {
        synchronized (mRemoteDeviceMap)
        {
            mRemoteDeviceMap.put(device.mAddress, device);
        }
        notifyDeviceListChanged();
    }

    private void removeRemoteDeviceList(String address)
    {
        BtDeviceRemote device = mRemoteDeviceMap.get(address);
        if (device != null)
        {
            device.onDestroy();
        }
        synchronized (mRemoteDeviceMap)
        {
            mRemoteDeviceMap.remove(address);
        }
        notifyDeviceListChanged();
    }

    private void clearRemoteDeviceList()
    {
        synchronized (mRemoteDeviceMap)
        {
            Collection<BtDeviceRemote> devices = mRemoteDeviceMap.values();
            Iterator<BtDeviceRemote> iterator = devices.iterator();
            while (iterator.hasNext())
            {
                BtDeviceRemote btDevice = (BtDeviceRemote) iterator.next();
                if (btDevice != null)
                {
                    btDevice.onDestroy();
                }
            }
            mRemoteDeviceMap.clear();
        }
        notifyDeviceListChanged();
    }

    boolean isIncallOnFront()
    {
        try
        {
            ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            List<RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
            if (rti != null && rti.get(0) != null)
            {
                return "com.chleon.ch10.btphone.BTPhoneIncall".equalsIgnoreCase(rti.get(0).topActivity.getClassName());
            }
            else
            {
                return false;
            }
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private void notifyBtA2DPSinkStateChanged(int state)
    {
        WLog.d(TAG, "JonPHONE: notifyBtA2DPSinkStateChanged state:" + state);
        Intent intent = new Intent("com.chleon.bluetooth.a2dpsink.changed");
        intent.putExtra("status", state);
        try
        {
            mContext.sendBroadcast(intent);
        }
        catch (Exception e)
        {
        }
    }

    //////////////////////////////////////////////////////////
    public void deviceGetName(String address)
    {
        sendRequest(CMD_GET_REMOTE_DEVICE_NAME, address);
    }

    @Override
    public int getDeviceServiceState(String address, int profile)
    {
        synchronized (mRemoteDeviceMap)
        {
            BtDeviceRemote btDevice = mRemoteDeviceMap.get(address);
            if (btDevice != null)
            {
                switch (profile)
                {
                    case BTConstants.BT_MAP:
                        return btDevice.getMAPSvcState();
                    case BTConstants.BT_A2DP:
                        return btDevice.getA2DPSvcState();
                    case BTConstants.BT_HFP:
                        return btDevice.getHFPSvcState();
                    case BTConstants.BT_PBAP:
                        return btDevice.getPBAPSvcState();
                }
            }
        }
        return 0;
    }

    @Override
    public boolean deviceConnect(String address, int profile, boolean state)
    {
        WLog.d(TAG, "Jon: deviceConnect Enter,profile is:" + profile + ",state is:" + state);
        BtDeviceRemote btDevice;
        synchronized (mRemoteDeviceMap)
        {
            btDevice = mRemoteDeviceMap.get(address);
        }
        if (btDevice == null)
        {
            return false;
        }
        Object[] data = new Object[3];
        data[0] = address;
        data[1] = profile;
        data[2] = state;

        return (Boolean) sendRequest(CMD_DEVICE_CONNECT_DISCONNECT, data);
    }


    boolean devicePullPhoneBook(int storage, int type, int maxlist, int offset)
    {
        Object[] data = new Object[4];
        data[0] = storage;
        data[1] = type;
        data[2] = maxlist;
        data[3] = offset;
        return (Boolean) sendRequest(CMD_GET_PHONE_BOOK, data);
    }


    boolean deviceGetMsgList(int fold, int maxlist, int offset)
    {
        Object[] data = new Object[3];
        data[0] = fold;
        data[1] = maxlist;
        data[2] = offset;
        return (Boolean) sendRequest(CMD_GET_MSG_LIST, data);
    }

    boolean deviceGetMsgListCont()
    {
        return (Boolean) sendRequest(CMD_GET_MSG_LIST_CONT, null);
    }

    boolean deviceGetMsgListCmt()
    {
        WLog.d(TAG, "deviceGetMsgListCmt Enter");
        return (Boolean) sendRequest(CMD_GET_MSG_LIST_CMT, null);
    }

    boolean deviceGetMsg(String handler)
    {
        return (Boolean) sendRequest(CMD_GET_MSG, handler);
    }

    boolean deviceFinishPullPhoneBook()
    {
        return (Boolean) sendRequest(CMD_FINISH_PULL_PHONEBOOK, null);
    }

    boolean deviceSendMsg(int more, String msg)
    {
        Bundle b = new Bundle();
        b.putInt("more", more);
        b.putString("message", msg);
        return (Boolean) sendRequest(CMD_SEND_MSG, b);
    }

    void makeSureOnlyOneDeviceConnected(String connectAddress)
    {
        mCurrenAddr = connectAddress;
        if (connectAddress != null && !connectAddress.equals("00:00:00:00:00:00"))
        {
            mLastConnectedAddr = connectAddress;
        }

        // WLog.d(TAG,"Jon: AutoConnectedRunnable makeSureOnlyOneDeviceConnected mLastConnectedAddr is:"+mLastConnectedAddr);
        synchronized (mRemoteDeviceMap)
        {
            Collection<BtDeviceRemote> devices = mRemoteDeviceMap.values();
            Iterator<BtDeviceRemote> iterator = devices.iterator();
            while (iterator.hasNext())
            {
                BtDeviceRemote btDevice = (BtDeviceRemote) iterator.next();
                if (btDevice != null && !btDevice.mAddress.equals(mCurrenAddr))
                {
                    btDevice.resetSvcState();
                }
            }
        }
    }

    void handleHFPConnectStateChanged(Bundle b)
    {
        WLog.d(TAG, "handleHFPConnectStateChanged Enter");
        int status = b.getInt("status");
        String address = b.getString("bd");
        int profile = b.getInt("profile");
        WLog.d(TAG, "handleHFPConnectStateChanged status:" + status + ",address:" + address +
                ",profile:" + profile);
        if (status == 0)
        {
            mHFPConnectionTimeStamp = System.currentTimeMillis();
            makeSureOnlyOneDeviceConnected(address);
            if (mHFPState > 3)
            {
                mIncallBeforeConnection = true;
            }
        }
        else
        {
            mIncallBeforeConnection = false;
        }
        BtDeviceRemote btDevice;
        synchronized (mRemoteDeviceMap)
        {
            btDevice = mRemoteDeviceMap.get(address);
        }
        if (btDevice != null)
        {
            btDevice.setHFPConnectState(status, mHFPState);
        }
        if (status == 0)
        {
            new Thread()
            {
                @Override
                public void run()
                {
                    getCurrentCallInformation();
                }
            }.start();
        }
    }

    boolean getCurrentCallInformation()
    {
        return (Boolean) sendRequest(CMD_GET_CALL_INFO, null);
    }

    boolean needResetCallArray = false;

    void handleHFPServiceStateChanged(Bundle b)
    {
        int status = b.getInt("status");
        WLog.d(TAG, "handleHFPServiceStateChanged status:" + status);
        mHFPState = status;
        if (mCurrenAddr != null && mCurrenAddr.length() > 0)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.setHFPSvcState(status);
            }
        }

        if (mHFPState <= BtDeviceRemote.HFP_TL_SLC_CONNECTED)
        {
            if (mIsInCallState)
            {
                mIsInCallState = false;
            }
            mCurrentCall = null;
            synchronized (mCallStatusArray)
            {
                mCallStatusArray.clear();
            }
            notifyCallStateChanged();
        }
        else
        {
            if (!mIsInCallState)
            {
                mIsInCallState = true;
                WLog.d(TAG, "Jon: handleHFPServiceStateChanged mIsInCallState :" + mIsInCallState);
            }
            switch (mHFPState)
            {
                case BtDeviceRemote.HFP_TL_INCOMMINECALL:
                    break;
                case BtDeviceRemote.HFP_TL_OUTGOINGCALL:
                    break;
                case BtDeviceRemote.HFP_TL_ACTIVECALL:
                {
                    if (mCallStatusArray != null && mCallStatusArray.size() > 1)
                    {
                        needResetCallArray = true;
                    }
                    break;
                }
                case BtDeviceRemote.HFP_TL_TWCALLING:
                    break;
                case BtDeviceRemote.HFP_TL_TWCALLONHOLD:
                    break;
                case BtDeviceRemote.HFP_TL_TWMULTICALL:
                    break;
                case BtDeviceRemote.HFP_TL_TWCALLONHoldNoActive:
                {
                    if (mCallStatusArray != null && mCallStatusArray.size() > 1)
                    {
                        needResetCallArray = true;
                    }
                    break;
                }
            }
        }
    }

    void handleA2DPConnectStateChanged(Bundle b)
    {
        WLog.d(TAG, "handleA2DPConnectStateChanged Enter");
        int status = b.getInt("status");
        String address = b.getString("bd");
        WLog.d(TAG, "handleA2DPConnectStateChanged status:" + status + ",address:" + address);
        if (status == 0)
        {
            makeSureOnlyOneDeviceConnected(address);
        }
    }

    void handleA2DPServiceStateChanged(Bundle b)
    {
        WLog.d(TAG, "handleA2DPServiceStateChanged Enter");
        int status = b.getInt("status");
        WLog.d(TAG, "handleA2DPServiceStateChanged status:" + status);
        mA2DPState = status;
        if (mCurrenAddr != null && mCurrenAddr.length() > 0)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.setA2DPSvcState(status);
            }
        }
        mMediaInfo.playState = ApuBtUtils.convertPlayerStatusByA2DPState(status);
        if (mMediaInfo.setPlayActivity(mMediaInfo.playState))
        {
            notifyPlayerStatus(mMediaInfo.getPlayActivity());
        }
        if (status < BtDeviceRemote.A2DP_CONNECTED)
        {
            synchronized (mMediaInfo)
            {
                mMediaInfo.reset();
                mLastMusicTile = null;
                notifyPlayerMetaDataChanged();
                notifyMessageToMenu(true);
            }
        }
        else if (status == BtDeviceRemote.A2DP_CONNECTED)
        {
        }

    }

    void handlePBAPConnectStateChanged(Bundle b)
    {
        WLog.d(TAG, "handlePBAPConnectStateChanged Enter");
        int status = b.getInt("status");
        String address = b.getString("bd");
        WLog.d(TAG, "handlePBAPConnectStateChanged status:" + status + ",address:" + address);
        if (status == 0)
        {
            makeSureOnlyOneDeviceConnected(address);
        }
    }

    void handlePBAPServiceStateChanged(Bundle b)
    {
        WLog.d(TAG, "handlePBAPServiceStateChanged Enter");
        int status = b.getInt("status");
        WLog.d(TAG, "handlePBAPServiceStateChanged status:" + status);
        mPBAPState = status;
        if (mCurrenAddr != null && mCurrenAddr.length() > 0)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.setPBAPSvcState(status);
            }
        }
    }


    void handleMAPConnectStateChanged(Bundle b)
    {
        WLog.d(TAG, "handleMAPConnectStateChanged Enter");
        int status = b.getInt("status");
        String address = b.getString("bd");
        WLog.d(TAG, "handleMAPConnectStateChanged status:" + status + ",address:" + address);
        if (status == 0)
        {
            makeSureOnlyOneDeviceConnected(address);
        }

        BtDeviceRemote btDevice;
        synchronized (mRemoteDeviceMap)
        {
            btDevice = mRemoteDeviceMap.get(address);
        }
        if (btDevice != null)
        {
            if (status == 0)
            {
                btDevice.setMAPSvcState(BtDeviceRemote.MAP_CONNECTED); //connected
            }
            else
            {
                btDevice.setMAPSvcState(BtDeviceRemote.MAP_DISCONNECTED); //disconnected
            }
        }
    }

    void handleAVRCPConnectStateChanged(Bundle b)
    {
        WLog.d(TAG, "handleAVRCPConnectStateChanged Enter");
        int status = b.getInt("status");
        String address = b.getString("bd");
        WLog.d(TAG, "handleAVRCPConnectStateChanged status:" + status + ",address:" + address);
        if (status == 0)
        {
            makeSureOnlyOneDeviceConnected(address);
        }
    }

    void handlePhoneBookSzChanged(Bundle b)
    {
        WLog.d(TAG, "handlePhoneBookSzChanged Enter");
        int pbsz = b.getInt("pbsz");
        if (mCurrenAddr != null && mCurrenAddr.length() > 0 && pbsz > 0)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.phonebookSzChanged(pbsz);
            }
        }

    }

    void handleRemoteDevNameChanged(Bundle b)
    {
        WLog.d(TAG, "handleRemoteDevNameChanged Enter");
        int status = b.getInt("status");
        String address = b.getString("bd");
        if (status == 0)
        {
            String name = b.getString("name");
            WLog.d(TAG, "handleRemoteDevNameChanged status:" + status + ",address:" + address +
                    ",name:" + name);
            if (name != null)
            {
                BtDeviceRemote btDevice;
                synchronized (mRemoteDeviceMap)
                {
                    btDevice = mRemoteDeviceMap.get(address);
                }
                if (btDevice != null)
                {
                    btDevice.updateNameStoried(name);
                }
            }
        }
    }


    void handleLocalDevNameChanged(Bundle b)
    {
        WLog.d(TAG, "handleLocalDevNameChanged Enter");
        int status = b.getInt("status");
        if (status == 0)
        {
            mBtHostName = b.getString("name");
            WLog.d(TAG, "handleLocalDevNameChanged status:" + status + ",name:" + mBtHostName);
        }
    }

    synchronized void releaseRingPopCheckTimer()
    {
        if (ring_timer != null)
        {
            ring_timer.cancel();
        }
        ring_timer = null;
    }

    void handleAVRCPPlayCmdStatusChanged(Bundle b)
    {
        WLog.d(TAG, "handleAVRCPPlayCmdStatusChanged Enter");
        int status = b.getInt("status");
        if (status == 0)
        {
            setPlayerState(PLAYER_STATE_STARTING);
        }
    }

    void handleAVRCPPauseCmdStatusChanged(Bundle b)
    {
        WLog.d(TAG, "handleAVRCPPauseCmdStatusChanged Enter");
        int status = b.getInt("status");
        if (status == 0)
        {
            setPlayerState(PLAYER_STATE_PAUSING);
        }
    }

    void handlePairedListChanged(Bundle b)
    {
        WLog.d(TAG, "handlePairedListChanged Enter");
        int total = b.getInt("total");
        int index = b.getInt("index");
        String address = b.getString("bd");
        String name = b.getString("name");
        if (!isValidMacAddress(address))
        {
            return;
        }
        if (mRemoteDeviceMap.get(address) == null)
        {
            BtDeviceRemote device = new BtDeviceRemote(address, name);
            addRemoteDeviceList(device);
        }
        if (mCurrenAddr != null &&
                mCurrenAddr.length() > 0 &&
                mHFPState != -1)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.setHFPSvcState(mHFPState);
                btDevice.setA2DPSvcState(mA2DPState);
            }
        }

    }


    void handlePlayerStatus(Bundle b)
    {
        WLog.d(TAG, "handlePlayerStatus Enter");
        int status = b.getInt("status");
        mMediaInfo.playState = ApuBtUtils.convertPlayerStatus(status);
        if (mMediaInfo.setPlayActivity(mMediaInfo.playState))
        {
            notifyPlayerStatus(mMediaInfo.getPlayActivity());
        }
    }

    void handlePullPBCmtChanged(ArrayList<BtPBContact> contacts)
    {
        WLog.d(TAG, "handlePullPBCmtChanged Enter");
        if (mCurrenAddr != null && mCurrenAddr.length() > 0)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.pullPBCompleted(contacts);
            }
        }
    }

    void handleGetMsgIndChanged(Bundle b)
    {
        WLog.d(TAG, "handleGetMsgIndChanged Enter");
        int type = b.getInt("type");
        if (type == 0)
        {
            int listSize = b.getInt("listSize");
            int moreData = b.getInt("moreData");
            int length = b.getInt("length");
            String packet = b.getString("packet");
            if (mCurrenAddr != null && mCurrenAddr.length() > 0)
            {
                BtDeviceRemote btDevice;
                synchronized (mRemoteDeviceMap)
                {
                    btDevice = mRemoteDeviceMap.get(mCurrenAddr);
                }
                if (btDevice != null)
                {
                    btDevice.onMsgListDataInd(moreData, packet);
                }
            }
        }
        else
        {
            int listSize = b.getInt("listSize");
            int moreData = b.getInt("moreData");
            int length = b.getInt("length");
            String packet = b.getString("packet");
            if (mCurrenAddr != null && mCurrenAddr.length() > 0)
            {
                BtDeviceRemote btDevice;
                synchronized (mRemoteDeviceMap)
                {
                    btDevice = mRemoteDeviceMap.get(mCurrenAddr);
                }
                if (btDevice != null)
                {
                    btDevice.onMsgDataInd(moreData, packet);
                }
            }
        }
    }

    void handleGetMsgCmtChanged()
    {
        WLog.d(TAG, "handleGetMsgCmtChanged Enter");
        if (mCurrenAddr != null && mCurrenAddr.length() > 0)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.onGetMsgDataCmt();
            }
        }
    }

    void handlePushMsgIndChanged()
    {
        WLog.d(TAG, "handlePushMsgIndChanged Enter");
        if (mCurrenAddr != null && mCurrenAddr.length() > 0)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.onPushMsgDataCont();
            }
        }
    }


    void handlePushMsgCmtChanged()
    {
        WLog.d(TAG, "handlePushMsgCmtChanged Enter");
        if (mCurrenAddr != null && mCurrenAddr.length() > 0)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.onPushMsgDataCmt();
            }
        }
    }

    void handleHFAudioStatusChanged(Bundle b)
    {
        WLog.d(TAG, "handleHFAudioStatusChanged Enter");
        int status = b.getInt("status");
        mPhoneHFAudioConnected = status;
        Intent intent = new Intent("chleon.android.bluetooth.priv.mode");
        intent.putExtra("MODE", status);
        mContext.sendBroadcast(intent);
        // if(status == 0 && iPeripheral != null) {
        //      mMainThreadHandler.postDelayed(mCheckAUXStatusWarning, 1500);
        // }
    }

    void handleMessageEvtStatusChanged(Bundle b)
    {
        WLog.d(TAG, "handleMessageEvtStatusChanged Enter");
        String evt = b.getString("event");
        if (mCurrenAddr != null && mCurrenAddr.length() > 0)
        {
            BtDeviceRemote btDevice;
            synchronized (mRemoteDeviceMap)
            {
                btDevice = mRemoteDeviceMap.get(mCurrenAddr);
            }
            if (btDevice != null)
            {
                btDevice.onPushMsgEvt(evt);
            }
        }
    }

    boolean isValidMacAddress(String mac)
    {
        String patternMac = "^[A-F0-9]{2}(:[A-F0-9]{2}){5}$";
        if (Pattern.compile(patternMac).matcher(mac).find())
        {
            return true;
        }
        return false;
    }


    private void remove(IBinder binder)
    {
        synchronized (mRecords)
        {
            final int recordCount = mRecords.size();
            for (int i = 0; i < recordCount; i++)
            {
                if (mRecords.get(i).binder == binder)
                {
                    mRecords.remove(i);
                    return;
                }
            }
        }
    }

    /**
     * Register listener for Bluetooth.
     *
     * @param pkgForDebug, for debug
     * @param callback,    refer to TBoxListener for more info
     * @param events,      event apps care about
     * @param notifyNow,   whether notify immediately or not
     */

    public void listen(String pkgForDebug, IBluetoothListener callback, int events, boolean notifyNow)
    {

        // override pkgForDebug, we keep this parameter for the future
        pkgForDebug = mContext.getPackageManager().getNameForUid(Binder.getCallingUid());

        WLog.d(TAG, "listen pkg=" + pkgForDebug + " events=0x" + Integer.toHexString(events));
        if (events != BluetoothListener.LISTEN_NONE)
        {
            /* Checks permission and throws Security exception */
            // checkListenerPermission(events);

            synchronized (mRecords)
            {
                // register
                Record r = null;
                find_and_add:
                {
                    IBinder b = callback.asBinder();
                    final int N = mRecords.size();
                    for (int i = 0; i < N; i++)
                    {
                        r = mRecords.get(i);
                        if (b == r.binder)
                        {
                            break find_and_add;
                        }
                    }
                    r = new Record();
                    r.binder = b;
                    r.callback = callback;
                    r.pkgForDebug = pkgForDebug;
                    mRecords.add(r);
                }
                int send = events & (events ^ r.events);
                r.events = events;
                if (notifyNow)
                {
                    if ((events & BluetoothListener.LISTEN_CALL_STATE) != 0)
                    {
                        try
                        {
                            r.callback.onCallStateChanged(getCurrentCallState());
                        }
                        catch (RemoteException ex)
                        {
                            remove(r.binder);
                        }
                    }
                }
            }
        }
        else
        {
            remove(callback.asBinder());
        }
    }

    @Override
    public void addBluetoothPbapCallback(IBluetoothPbapCallback callback) throws RemoteException
    {
        this.pbapCallback = callback;
    }
}

