package com.service.bluetooth;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

class BtDeviceRemote
{
    public static final int BT_PAIR_STATE_SUCCESS = 0;
    public static final int BT_PAIR_STATE_FAILED = 1;

    public static final int MAX_CHECK_CONNECT_TIMES = 10;

    int max_recheck_connect_time = 0;

    String mName;
    String mAddress;
    int mBondState;
    int mConnectState;

    BtManagerService mService;
    Context mContext;

    // SVC STATE
    public static final int SVC_CONNECTED = 1;
    public static final int SVC_DISCONNECTED = 0;

    // SVC define
    public static final int SVC_PHONE = 1;
    public static final int SVC_DUN = 2;
    public static final int SVC_A2DP_SNK = 3;
    public static final int SVC_AVRCP = 5;
    public static final int SVC_SPP = 7;
    public static final int SVC_PAN_U = 8;
    public static final int SVC_MAP = 9;
    public static final int SVC_PAN_GN = 11;
    public static final int SVC_PAN_NAP = 12;
    public static final int SVC_HID_DEVICE = 15;

    // EVENT
    static final int EVT_HFP_SVC_STATE_CHANGED = 0;
    static final int EVT_A2DP_SVC_STATE_CHANGED = 1;
    static final int EVT_PBAP_SVC_STATE_CHANGED = 2;
    static final int EVT_MAP_SVC_STATE_CHANGED = 3;

    static final int ACTION_GET_NAME = 100;
    static final int ACTION_GET_NAME_ONCE = 101;
    static final int ACTION_DISCONNECT_SVC_TIMEOUT = 102;
    static final int ACTION_RECHECK_CONNECT_SVC = 103;

    // HFP SVC STATE
    static final int HFP_TL_UNKNOWN = -1;
    static final int HFP_TL_READY = 1;
    static final int HFP_TL_SLC_CONNECTING = 2;
    static final int HFP_TL_SLC_CONNECTED = 3;
    static final int HFP_TL_INCOMMINECALL = 4;
    static final int HFP_TL_OUTGOINGCALL = 5;
    static final int HFP_TL_ACTIVECALL = 6;
    static final int HFP_TL_TWCALLING = 7;
    static final int HFP_TL_TWCALLONHOLD = 8;
    static final int HFP_TL_TWMULTICALL = 9;
    static final int HFP_TL_TWCALLONHoldNoActive = 10;

    // HFP Connect STATE
    static final int HFP_CONNECT_UNKNOWN = -1;
    static final int HFP_CONNECT_SUCCESS = 0;
    static final int HFP_CONNECT_SDP_FAIL = 1;
    static final int HFP_CONNECT_SLC_FAILED = 2;
    static final int HFP_CONNECT_FAILED_BUSY = 3;
    static final int HFP_CONNECT_FAILED = 4;
    static final int HFP_CONNECT_SVR_CHANNEL_NOT_REG = 5;
    static final int HFP_CONNECT_TIMEOUT = 6;
    static final int HFP_CONNECT_REJECTED = 7;
    static final int HFP_CONNECT_NORMAL_DISCONNECT = 8;
    static final int HFP_CONNECT_ABNORMAL_DISCONNECT = 9;
    static final int HFP_CONNECT_FAILED_BAD_PARAMS = 10;

    static final int MAX_GET_NAME_TIME = 5;
    int mMaxGetNameTimes = MAX_GET_NAME_TIME;

    // int mHFPConnectState = HFP_CONNECT_UNKNOWN;
    int mHFPSvcState = HFP_TL_UNKNOWN;

    // A2DP SVC STATE
    static final int A2DP_UNKNOWN = -1;
    static final int A2DP_READY = 1;
    static final int A2DP_CONNECTING = 2;
    static final int A2DP_CONNECTED = 3;
    static final int A2DP_STREAMING = 4;

    // int mA2DPConnectState = 0;//success;
    int mA2DPSvcState = A2DP_UNKNOWN;//

    // PBAP SVC STATE
    static final int PBAP_UNKNOWN = -1;
    static final int PBAP_READY = 1;
    static final int PBAP_CONNECTING = 2;
    static final int PBAP_CONNECTED = 3;
    static final int PBAP_DOWNLOADING = 4;
    static final int PBAP_DISCONNECTING = 5;

    // int mPBAPConnctState = 0;//success;
    int mPBAPSvcState = PBAP_UNKNOWN;//

    BtPhoneBook mPhoneBook;
    // ArrayList<BtPBContact> mPulledPBList;
    BtMessages mMsgs;

    // MAP SVC STATE
    static final int MAP_DISCONNECTED = 0;
    static final int MAP_CONNECTED = 1;

    // only for compatible
    public final static int MAP_CON_STATE_IDLE = 0;
    public final static int MAP_CON_STATE_INIT = 1;
    public final static int MAP_CON_STATE_SVC_FAILED = 2;
    public final static int MAP_CON_STATE_SVC_SUCCESS = 3;

    int mMAPSvcState = MAP_DISCONNECTED;//

    MyHandler mHandler;

    private static final String TAG = "Jon: BtDeviceRemote";

    private boolean isSupportMapFeature()
    {
        return false;
    }

    public BtDeviceRemote(String address)
    {
        mAddress = address;
        mName = mAddress;
        mService = BtManagerService.getDefault();
        mContext = mService.mContext;
        mBondState = BluetoothDevice.BOND_BONDED;
        mConnectState = BluetoothDevice.CONNECT_DISCONNECTED;
        onCreate();
    }

    public BtDeviceRemote(String address, String name)
    {
        mAddress = address;
        mService = BtManagerService.getDefault();
        mContext = mService.mContext;
        mBondState = BluetoothDevice.BOND_BONDED;
        mConnectState = BluetoothDevice.CONNECT_DISCONNECTED;
        updateNameStoried(name);
        onCreate();
    }

    void onCreate()
    {
        max_recheck_connect_time = 0;
        mHandler = new MyHandler(mService.mBtMainThread.getLooper());
        mPhoneBook = new BtPhoneBook(this);
        if (isSupportMapFeature())
        {
            mMsgs = new BtMessages(this);
        }
        else
        {
            mMsgs = null;
        }
        if (mAddress.equals(mName))
        {
            mHandler.sendEmptyMessage(ACTION_GET_NAME_ONCE);
        }
    }

    void onDestroy()
    {
        max_recheck_connect_time = 0;
        mHandler.removeMessages(ACTION_RECHECK_CONNECT_SVC);
        mHandler.removeMessages(ACTION_GET_NAME_ONCE);
        mHandler.removeMessages(ACTION_GET_NAME);
        mHandler.removeMessages(ACTION_DISCONNECT_SVC_TIMEOUT);
        mHFPSvcState = HFP_TL_UNKNOWN;
        mA2DPSvcState = A2DP_UNKNOWN;
        mPBAPSvcState = PBAP_UNKNOWN;
        mMAPSvcState = MAP_DISCONNECTED;
        if (mConnectState != BluetoothDevice.CONNECT_DISCONNECTED)
        {
            onEvtChange(EVT_HFP_SVC_STATE_CHANGED);
            onEvtChange(EVT_A2DP_SVC_STATE_CHANGED);
            onEvtChange(EVT_PBAP_SVC_STATE_CHANGED);
            onEvtChange(EVT_MAP_SVC_STATE_CHANGED);
        }
    }

    @Override
    public String toString()
    {
        return "BtDeviceRemote [addr]:" + mAddress + ",[name]:" + mName + ",[pair]:" + mBondState + ",[conn]:" + mConnectState;
    }

    class MyHandler extends Handler
    {
        public MyHandler(Looper loop)
        {
            super(loop);
        }

        @Override
        public void handleMessage(Message message)
        {
            handleEvt(message.what);
        }
    }

    void onEvtChange(int evt)
    {
        mHandler.sendEmptyMessage(evt);
    }

    /**
     * Used as an int extra field in intents.
     * Contains the bond state of the remote device.
     * <p/>
     */
    boolean setBondState(int state)
    {
        if (mBondState == state)
        {
            return true;
        }
        mBondState = state;
        return true;
    }

    int getBondState()
    {
        return mBondState;
    }

    boolean isPaired()
    {
        return mBondState == BTConstants.BT_PAIRING_STATUS_SUCCEEDED;
    }

    /**
     * @return value is one of {@link BluetoothDevice#CONNECT_CONNECTED},
     * {@link BluetoothDevice#CONNECT_CONNECTING}
     * {@link BluetoothDevice#CONNECT_DISCONNECTED} or
     * {@link BluetoothDevice#CONNECT_DISCONNECTING},
     */
    int getState()
    {
        WLog.d(TAG, "Jon: [B] mName is:" + mName + ",mAddress:" + mAddress + ",mConnectState is:" + mConnectState);
        return mConnectState;
    }

    void handleEvt(int event)
    {
        switch (event)
        {
            case EVT_HFP_SVC_STATE_CHANGED:
            case EVT_A2DP_SVC_STATE_CHANGED:
            {
                updateConnectState();
                break;
            }
            case EVT_PBAP_SVC_STATE_CHANGED:
            {
                onPBAPSvcChanged();
                break;
            }
            case EVT_MAP_SVC_STATE_CHANGED:
            {
                onMAPSvcChanged();
                break;
            }
            case ACTION_GET_NAME_ONCE:
            {
                if (mName == null || mAddress.equals(mName))
                {
                    new Thread()
                    {
                        public void run()
                        {
                            WLog.d(TAG, "JonXXXX: try get Name,mAddress:" + mAddress);
                            mService.deviceGetName(mAddress);
                        }
                    }.start();
                }
            }
            case ACTION_GET_NAME:
            {
                WLog.d(TAG, "ACTION_GET_NAME mName is:" + mName + ",mAddress is:" + mAddress);
                if (mName == null || mAddress.equals(mName))
                {
                    new Thread()
                    {
                        public void run()
                        {
                            WLog.d(TAG, "JonXXXX: try get Name,mAddress:" + mAddress);
                            mService.deviceGetName(mAddress);
                            if (mMaxGetNameTimes > 0)
                            {
                                mMaxGetNameTimes--;
                                mHandler.sendEmptyMessageDelayed(ACTION_GET_NAME, 10000);
                            }
                        }
                    }.start();
                }
                break;
            }
            case ACTION_DISCONNECT_SVC_TIMEOUT:
                WLog.d(TAG, "Disconnect Svc timeout");
                break;
            case ACTION_RECHECK_CONNECT_SVC:
                WLog.d(TAG, "Jon: [BT] recheck connect Svc");
                if (max_recheck_connect_time > 0)
                {
                    new Thread()
                    {
                        public void run()
                        {
                            if (getPBAPSvcState() == 0 && (mConnectState == BluetoothDevice.CONNECT_CONNECTING || mConnectState == BluetoothDevice.CONNECT_CONNECTED))
                            {
                                mService.deviceConnect(mAddress, BTConstants.BT_PBAP, true);
                                try
                                {
                                    Thread.sleep(200);
                                }
                                catch (InterruptedException e)
                                {
                                }
                            }
                            if (isSupportMapFeature())
                            {
                                if (getMAPSvcState() == 0 && (mConnectState == BluetoothDevice.CONNECT_CONNECTING || mConnectState == BluetoothDevice.CONNECT_CONNECTED))
                                {
                                    mService.deviceConnect(mAddress, BTConstants.BT_MAP, true);
                                    try
                                    {
                                        Thread.sleep(200);
                                    }
                                    catch (InterruptedException e)
                                    {
                                    }
                                }
                            }
                            if (getA2DPSvcState() == 0 && (mConnectState == BluetoothDevice.CONNECT_CONNECTING || mConnectState == BluetoothDevice.CONNECT_CONNECTED))
                            {
                                mService.deviceConnect(mAddress, BTConstants.BT_A2DP, true);
                                try
                                {
                                    Thread.sleep(200);
                                }
                                catch (InterruptedException e)
                                {
                                }
                            }
                            if (getHFPSvcState() == 0 && (mConnectState == BluetoothDevice.CONNECT_CONNECTING || mConnectState == BluetoothDevice.CONNECT_CONNECTED))
                            {
                                mService.deviceConnect(mAddress, BTConstants.BT_HFP, true);
                                try
                                {
                                    Thread.sleep(200);
                                }
                                catch (InterruptedException e)
                                {
                                }
                            }
                            if (max_recheck_connect_time > 0 && (mConnectState == BluetoothDevice.CONNECT_CONNECTING || mConnectState == BluetoothDevice.CONNECT_CONNECTED))
                            {
                                mHandler.sendEmptyMessageDelayed(ACTION_RECHECK_CONNECT_SVC, 4000);
                                max_recheck_connect_time--;
                            }
                        }
                    }.start();
                }
                else
                {
                    if (mConnectState == BluetoothDevice.CONNECT_CONNECTING)
                    {
                        WLog.d(TAG, " recheck connect timout,mConnectState:" + mConnectState);
                        updateConnectState();
                    }
                }
                break;
            default:
            {
                WLog.d(TAG, "Unkown @ConnectedState evt:" + event);
                break;
            }
        }
    }

    String getName()
    {
        WLog.d(TAG, "getName Enter");
        mName = getNameStoried();
        if (mName == null || mAddress.equals(mName))
        {
            mName = mAddress;
            mMaxGetNameTimes = MAX_GET_NAME_TIME;
            mHandler.removeMessages(ACTION_GET_NAME);
            mHandler.sendEmptyMessageDelayed(ACTION_GET_NAME, 2000);
        }
        return mName;
    }

    String getNameStoried()
    {
        String name = Settings.System.getString(mContext.getContentResolver(), mAddress);
        WLog.d(TAG, "getNameStoried Enter,mAddress:" + mAddress + ",name:" + name);
        return name;
    }

    void updateNameStoried(String name)
    {
        Settings.System.putString(mContext.getContentResolver(), mAddress, name);
        WLog.d(TAG, "updateNameStoried Enter,mAddress:" + mAddress + ",name:" + name);
        mName = name;
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

    boolean connect(boolean on)
    {
        if (on)
        {
            mConnectState = BluetoothDevice.CONNECT_CONNECTING;
            mHandler.removeMessages(ACTION_RECHECK_CONNECT_SVC);
            sleep(100);
            mService.deviceConnect(mAddress, BTConstants.BT_A2DP, on);
            sleep(100);
            mService.deviceConnect(mAddress, BTConstants.BT_HFP, on);
        }
        else
        {
            mConnectState = BluetoothDevice.CONNECT_DISCONNECTING;
            mHandler.removeMessages(ACTION_RECHECK_CONNECT_SVC);
            sleep(100);
            mService.deviceConnect(mAddress, BTConstants.BT_A2DP, on);
            sleep(100);
            mService.deviceConnect(mAddress, BTConstants.BT_HFP, on);
            sleep(100);
            mService.deviceConnect(mAddress, BTConstants.BT_PBAP, on);
            mPhoneBook.onPBAPSvcDisconnecte();
            if (isSupportMapFeature())
            {
                sleep(100);
                mMsgs.disconnectService();
            }
            mHandler.sendEmptyMessageDelayed(ACTION_DISCONNECT_SVC_TIMEOUT, 2500);
        }
        return true;
    }

    boolean pullPhoneBook(int storage, int type, int maxlist, int offset)
    {
        return mService.devicePullPhoneBook(storage, type, maxlist, offset);
    }

    void setHFPConnectState(int state, int hfSvcState)
    {
        // mHFPConnectState = state;
        if (state == 0)
        {
            if (mHFPSvcState == HFP_TL_UNKNOWN && hfSvcState != HFP_TL_UNKNOWN)
            {
                setHFPSvcState(hfSvcState);
            }
        }
    }

    void setHFPSvcState(int state)
    {
        WLog.d(TAG, "Jon: [B] setHFPSvcState:" + state);
        boolean report = true;
        if (mHFPSvcState < HFP_TL_SLC_CONNECTED && state >= HFP_TL_SLC_CONNECTED)
        {
            if (getPBAPSvcState() != SVC_CONNECTED || getMAPSvcState() != SVC_CONNECTED)
            {
                max_recheck_connect_time = MAX_CHECK_CONNECT_TIMES;
                mHandler.removeMessages(ACTION_RECHECK_CONNECT_SVC);
                mHandler.sendEmptyMessageDelayed(ACTION_RECHECK_CONNECT_SVC, 1000);
            }
        }
        else if (state < HFP_TL_SLC_CONNECTED && mHFPSvcState >= HFP_TL_SLC_CONNECTED)
        {
            if (getPBAPSvcState() != SVC_DISCONNECTED)
            {
                new Thread()
                {
                    public void run()
                    {
                        mHandler.removeMessages(ACTION_RECHECK_CONNECT_SVC);
                        if (isSupportMapFeature())
                        {
                            mMsgs.disconnectService();
                            try
                            {
                                Thread.sleep(200);
                            }
                            catch (InterruptedException e)
                            {
                            }
                        }
                        mService.deviceConnect(mAddress, BTConstants.BT_PBAP, false);
                    }
                }.start();
            }
        }
        if (mHFPSvcState == state)
        {
            return;
        }

        if (mHFPSvcState >= HFP_TL_SLC_CONNECTED && state >= HFP_TL_SLC_CONNECTED || mHFPSvcState < HFP_TL_SLC_CONNECTED && state < HFP_TL_SLC_CONNECTED)
        {
            report = false;
        }

        if (mHFPSvcState == HFP_TL_SLC_CONNECTING && state < HFP_TL_SLC_CONNECTING)
        {
            if (mConnectState == BluetoothDevice.CONNECT_CONNECTING)
            {
                report = true;
            }
        }

        mHFPSvcState = state;
        if (report)
        {
            onEvtChange(EVT_HFP_SVC_STATE_CHANGED);
        }
    }

    int getHFPSvcState()
    {
        if (mHFPSvcState >= HFP_TL_SLC_CONNECTED)
        {
            return SVC_CONNECTED; // connected
        }
        else
        {
            return SVC_DISCONNECTED; // disconnected
        }
    }

    void setA2DPSvcState(int state)
    {
        WLog.d(TAG, "Jon: [B] setA2DPSvcState:" + state);
        if (mA2DPSvcState != state)
        {
            mA2DPSvcState = state;
            onEvtChange(EVT_A2DP_SVC_STATE_CHANGED);
        }
        if (mA2DPSvcState < A2DP_CONNECTED && state >= A2DP_CONNECTED)
        {
            max_recheck_connect_time = MAX_CHECK_CONNECT_TIMES;
            mHandler.removeMessages(ACTION_RECHECK_CONNECT_SVC);
            mHandler.sendEmptyMessageDelayed(ACTION_RECHECK_CONNECT_SVC, 1000);
        }
    }

    int getA2DPSvcState()
    {
        if (mA2DPSvcState >= A2DP_CONNECTED)
        {
            return SVC_CONNECTED;
        }
        else
        {
            return SVC_DISCONNECTED;
        }
    }

    void setPBAPSvcState(int state)
    {
        boolean report = true;
        if (mPBAPSvcState >= PBAP_CONNECTED && state >= PBAP_CONNECTED || mPBAPSvcState < PBAP_CONNECTED && state < PBAP_CONNECTED)
        {
            report = false;
        }
        if (mPBAPSvcState != state)
        {
            mPBAPSvcState = state;
            if (report)
            {
                onEvtChange(EVT_PBAP_SVC_STATE_CHANGED);
            }
        }
    }

    int getPBAPSvcState()
    {
        if (mPBAPSvcState >= PBAP_CONNECTED)
        {
            return SVC_CONNECTED;
        }
        else
        {
            return SVC_DISCONNECTED;
        }
    }

    void setMAPSvcState(int state)
    {
        if (isSupportMapFeature())
        {
            if (mMAPSvcState != state)
            {
                mMAPSvcState = state;
                onEvtChange(EVT_MAP_SVC_STATE_CHANGED);
            }
        }
    }

    int getMAPSvcState()
    {
        if (isSupportMapFeature())
        {
            return (mMAPSvcState == MAP_CONNECTED) ? MAP_CON_STATE_SVC_SUCCESS : MAP_CON_STATE_IDLE;
        }
        return MAP_CON_STATE_IDLE;
    }

    void onPBAPSvcChanged()
    {
        switch (mPBAPSvcState)
        {
            case PBAP_UNKNOWN:
            case PBAP_READY:
            case PBAP_CONNECTING:
            default:
                mPhoneBook.onPBAPSvcDisconnecte();
                break;
            case PBAP_CONNECTED:
                mPhoneBook.onPBAPSvcConnected();
                break;
            case PBAP_DOWNLOADING:
                break;
            case PBAP_DISCONNECTING:
                break;
        }
    }

    void onMAPSvcChanged()
    {
        if (isSupportMapFeature())
        {
            switch (mMAPSvcState)
            {
                case MAP_DISCONNECTED:
                    notifySvcChanged(SVC_MAP, 0);
                    mMsgs.onMAPSvcDisconnecte();
                    break;
                case MAP_CONNECTED:
                    notifySvcChanged(SVC_MAP, 1);
                    mMsgs.onMAPSvcConnected();
                    break;
                default:
                    break;
            }
        }
    }

    void updateConnectState()
    {
        if (mHFPSvcState >= HFP_TL_SLC_CONNECTED || mA2DPSvcState >= A2DP_CONNECTED)
        {
            mConnectState = BluetoothDevice.CONNECT_CONNECTED;
        }
        else if (mHFPSvcState == HFP_TL_SLC_CONNECTING || mHFPSvcState == A2DP_CONNECTING)
        {
            mConnectState = BluetoothDevice.CONNECT_CONNECTING;
        }
        else
        {
            mHandler.removeMessages(ACTION_DISCONNECT_SVC_TIMEOUT);
            mConnectState = BluetoothDevice.CONNECT_DISCONNECTED;
            mPBAPSvcState = PBAP_UNKNOWN;
            mMAPSvcState = MAP_DISCONNECTED;
        }

        notifyBtDeviceStateChanged();

        if (mHFPSvcState >= HFP_TL_SLC_CONNECTED)
        {
            notifySvcChanged(SVC_PHONE, 1);
        }
        else
        {
            notifySvcChanged(SVC_PHONE, 0);
        }

        if (mA2DPSvcState >= A2DP_CONNECTED)
        {
            notifySvcChanged(SVC_A2DP_SNK, 1);
        }
        else
        {
            notifySvcChanged(SVC_A2DP_SNK, 0);
        }
    }

    void notifyBtDeviceStateChanged()
    {
        try
        {
            Intent intent = new Intent(BluetoothDevice.ACTION_STATE_CHANGED);
            intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(BluetoothDevice.EXTRA_STATE, mConnectState);
            BluetoothDevice btDevice = BluetoothManager.getDefault(mContext).getRemoteDevice(mAddress);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, btDevice);
            mContext.sendStickyBroadcast(intent);
        }
        catch (Exception ex)
        {
            WLog.e(TAG, "notifyBtDeviceStateChanged exception !!!!");
            ex.printStackTrace();
        }
    }

    void notifySvcChanged(int svc, int connected)
    {
        WLog.d(TAG, "Jon: [B] notifySvcChanged svc is:" + svc + ",connected is:" + connected);
        Intent intent = new Intent("chleon.android.bluetooth.device.svc.changed");
        intent.putExtra("SVC", svc);
        intent.putExtra("STATE", connected);
        mContext.sendBroadcast(intent);
    }

    void phonebookSzChanged(int sz)
    {
        mPhoneBook.onPhoneBookSzChanged(sz);
    }


    void pullPBCompleted(ArrayList<BtPBContact> contacts)
    {
        mPhoneBook.onPulledCompleted(contacts);
    }

    int getPhoneBookSyncedStatus()
    {
        return mPhoneBook.getPhoneBookSyncedStatus();
    }

    void getPhoneBookByManual()
    {
        mPhoneBook.getPhoneBookByManual();
    }

    int getPhoneBookSyncProgress()
    {
        return mPhoneBook.getPhoneBookSyncProgress();
    }

    void onPBSycSettingsChanged()
    {
        if (isCustomBtSwitchOn() && getPBAPSvcState() == 0)
        {
            new Thread()
            {
                public void run()
                {
                    WLog.d(TAG, "JonXXXX: try connect PBAP :" + mAddress);
                    mService.deviceConnect(mAddress, BTConstants.BT_PBAP, true);
                }
            }.start();
        }
        else
        {
            mPhoneBook.onPBSycSettingsChanged();
        }
    }

    void onCallStateChanged(int callState)
    {
        mPhoneBook.onCallStateChanged(callState);
    }

    void onMsgListDataInd(int moreData, String packet)
    {
        if (isSupportMapFeature())
        {
            mMsgs.onMsgListDataIndChanged(moreData, packet);
        }
    }

    void onMsgDataInd(int moreData, String packet)
    {
        if (isSupportMapFeature())
        {
            mMsgs.onMsgDataIndChanged(moreData, packet);
        }
    }

    void onGetMsgDataCmt()
    {
        if (isSupportMapFeature())
        {
            mMsgs.onGetMsgDataCmt();
        }
    }

    void onPushMsgDataCmt()
    {
        if (isSupportMapFeature())
        {
            mMsgs.onPushMsgDataCmt();
        }
    }

    void onPushMsgDataCont()
    {
        if (isSupportMapFeature())
        {
            mMsgs.onPushMsgDataCont();
        }
    }

    void onPushMsgEvt(String evt)
    {
        if (isSupportMapFeature())
        {
            mMsgs.onPushMsgEvt(evt);
        }
    }

    // /Message
    boolean getMsgList(int fold, int maxlist, int offset)
    {
        WLog.d(TAG, "getMsgList Enter");
        if (isSupportMapFeature())
        {
            return mService.deviceGetMsgList(fold, maxlist, offset);
        }
        return false;
    }

    boolean getMsgListCont()
    {
        WLog.d(TAG, "getMsgListCont Enter");
        if (isSupportMapFeature())
        {
            return mService.deviceGetMsgListCont();
        }
        return false;
    }

    boolean getMsgListCmt()
    {
        WLog.d(TAG, "getMsgListCmt Enter");
        if (isSupportMapFeature())
        {
            return mService.deviceGetMsgListCmt();
        }
        return false;
    }

    boolean getMsg(String handler)
    {
        WLog.d(TAG, "getMsg Enter");
        if (isSupportMapFeature())
        {
            return mService.deviceGetMsg(handler);
        }
        return false;
    }

    boolean finishPullPhonebook()
    {
        if (isSupportMapFeature())
        {
            return mService.deviceFinishPullPhoneBook();
        }
        return false;
    }

    boolean sendMesssage(int more, String msg)
    {
        WLog.d(TAG, "sendMesssage Enter");
        if (isSupportMapFeature())
        {
            return mService.deviceSendMsg(more, msg);
        }
        return false;
    }

    List<BluetoothMessage> getMessages()
    {
        WLog.d(TAG, "getMsgListCmt Enter");
        if (isSupportMapFeature())
        {
            return mMsgs.getMessages();
        }
        return null;
    }

    boolean retriveMapMessage(int accountId, int msgId)
    {
        WLog.d(TAG, "retriveMapMessage Enter");
        if (isSupportMapFeature())
        {
            return mMsgs.retriveMapMessage(accountId, msgId);
        }
        return false;
    }

    boolean sendMapMessage(BluetoothMessage msg)
    {
        if (isSupportMapFeature())
        {
            return mMsgs.sendMapMessage(msg);
        }
        return false;
    }

    void resetSvcState()
    {
        if (mConnectState != BluetoothDevice.CONNECT_DISCONNECTED)
        {
            WLog.d(TAG, "resetSvcState Enter,mDevice is:" + mName);
            max_recheck_connect_time = 0;
            mHandler.removeMessages(ACTION_RECHECK_CONNECT_SVC);
            new Thread()
            {
                public void run()
                {
                    if (mConnectState == BluetoothDevice.CONNECT_CONNECTED)
                    {
                        if (getPBAPSvcState() > 0)
                        {
                            mService.deviceConnect(mAddress, BTConstants.BT_PBAP, false);
                        }
                        if (isSupportMapFeature())
                        {
                            if (getMAPSvcState() > 0)
                            {
                                mService.deviceConnect(mAddress, BTConstants.BT_MAP, false);
                            }
                        }
                        if (getA2DPSvcState() > 0)
                        {
                            mService.deviceConnect(mAddress, BTConstants.BT_A2DP, false);
                        }
                        if (getHFPSvcState() > 0)
                        {
                            mService.deviceConnect(mAddress, BTConstants.BT_HFP, false);
                        }
                    }
                    mHFPSvcState = HFP_TL_UNKNOWN;
                    mA2DPSvcState = A2DP_UNKNOWN;
                    mPBAPSvcState = PBAP_UNKNOWN;
                    mMAPSvcState = MAP_DISCONNECTED;
                    onEvtChange(EVT_HFP_SVC_STATE_CHANGED);
                    onEvtChange(EVT_A2DP_SVC_STATE_CHANGED);
                    onEvtChange(EVT_PBAP_SVC_STATE_CHANGED);
                    onEvtChange(EVT_MAP_SVC_STATE_CHANGED);
                }
            }.start();
        }
    }

    boolean isCustomBtSwitchOn()
    {
        return (Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.BLUETOOTH_ON, 0) == 1);
    }
}

