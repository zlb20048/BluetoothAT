package com.service.bluetooth;

import android.accounts.Account;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Patterns;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;

public class BtPhoneBook
{
    final String TAG = "Jon: BtPhoneBook";
    private AsyncQueryHandler mContactsAsyncQueryHandler;

    BtDeviceRemote mDevice;
    int mState;
    int mPbLoadingPercent = 0;

    int STORAGE_LOCAL = 1;
    int STORAGE_SIM = 2;

    int FOLD_PB = 1; // main phone book
    int FOLD_ICH = 2; // incomming calls
    int FOLD_OCH = 3; // outgoing calls
    int FOLD_MCH = 4; // missed calls
    int FOLD_CCH = 5; // ALL Call logs

    final static int STATE_INITED = 0;
    final static int STATE_READY = 1;
    final static int STATE_DOWNLADING = 2;

    static final int OP_NULL = 0x00;
    static final int OP_PB = 0x01;
    static final int OP_ICH = 0x02;
    static final int OP_OCH = 0x04;
    static final int OP_MCH = 0x08;
    static final int OP_CCH = 0x10;
    static final int OP_DEL_PB = 0x20;
    static final int OP_PB_SZ = 0x40;
    static final int OP_DEL_CALLOG_ONLY = 0x80;

    static final int OP_ICH_ONCE = 0x100;
    static final int OP_OCH_ONCE = 0x200;
    static final int OP_MCH_ONCE = 0x400;


    int mOp = OP_NULL;
    int mOpFlag = OP_NULL;

    final static int EVT_PBAP_SVC_CONNECTED = 0;
    final static int EVT_PBAP_SVC_DISCONNECTED = 1;
    final static int EVT_PULL_COMPLETED = 2;
    final static int EVT_PULLPB_SETTINGS_CHANGED = 3;
    final static int EVT_STATE_CHANGED = 4;
    final static int EVT_PB_SYNC_SETTINGS_CHANGED = 5;
    final static int EVT_CALL_STATE_CHANGE_IDLE = 6;
    final static int EVT_PROCCESS_TIMEOUT = 7;
    final static int EVT_PULLPB_BY_MANUAL = 8;
    final static int EVT_PULLCALLLOG_ONLY = 9;


    final static int MAX_LIST_NUM = 65535;
    final static int MAX_PB_PULL_ITEMS = 20;
    final static int MAX_WAIT_TIME = 60;

    final static int MAX_CALLLOG_ITEMS = 20;

    int mPullPBOffSet = 0;

    final Object mSyncLock = new Object();
    boolean mPullComplete = true;

    ArrayList<BtPBContact> mPullList = null;
    int mPbSz = 0;

    Context mContext;
    private final Account mAccount;

    private int mPhoneBookSycedStatus = STATE_CONTACT_SYNC_IDLE;
    private int mContactSyncProccess = 0;

    static final int STATE_CONTACT_SYNC_UNKNOWN = -2;
    static final int STATE_CONTACT_SYNC_ERROR = -1;
    static final int STATE_CONTACT_SYNC_IDLE = 0;
    static final int STATE_CONTACT_SYNC_START = 1;
    static final int STATE_CONTACT_SYNC_FINISHED = 2;
    static final int STATE_CONTACT_SYNC_VISIBLE = 3;


    MyHandler mHandler;

    static final int MAX_OP_TIMEOUT = 60;

    public BtPhoneBook(BtDeviceRemote device)
    {
        mDevice = device;
        mContext = mDevice.mService.mContext;
        mAccount = new Account(mDevice.mAddress, "com.chleon.bluetooth.sync");
        mHandler = new MyHandler(mDevice.mService.mBtMainThread.getLooper());
        mContactsAsyncQueryHandler = new ContactsQueryHandler(mContext.getContentResolver());
        initedState();
    }

    public void onPBAPSvcDisconnecte()
    {
        WLog.d(TAG, "onPBAPSvcDisconnecte Enter");
        onEvtChange(EVT_PBAP_SVC_DISCONNECTED);
    }

    public void onPBAPSvcConnected()
    {
        WLog.d(TAG, "onPBAPSvcConnected Enter");
        onEvtChange(EVT_PBAP_SVC_CONNECTED);
    }

    public void onPulledCompleted(ArrayList<BtPBContact> contacts)
    {
        mPullList = contacts;
        onEvtChange(EVT_PULL_COMPLETED);
    }

    public void onPhoneBookSzChanged(int sz)
    {
        mPbSz = sz;
        WLog.d(TAG, "Jon: onPhoneBookSzChanged pbsz is:" + mPbSz);
    }

    int getNextOp()
    {
        if ((OP_DEL_PB & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_DEL_PB));
            return OP_DEL_PB;
        }
        else if ((OP_DEL_CALLOG_ONLY & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_DEL_CALLOG_ONLY));
            return OP_DEL_CALLOG_ONLY;
        }
        else if ((OP_PB_SZ & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_PB_SZ));
            return OP_PB_SZ;
        }
        else if ((OP_ICH_ONCE & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_ICH_ONCE));
            return OP_ICH_ONCE;
        }
        else if ((OP_OCH_ONCE & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_OCH_ONCE));
            return OP_OCH_ONCE;
        }
        else if ((OP_MCH_ONCE & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_MCH_ONCE));
            return OP_MCH_ONCE;
        }
        else if ((OP_ICH & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_ICH));
            return OP_ICH;
        }
        else if ((OP_OCH & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_OCH));
            return OP_OCH;
        }
        else if ((OP_MCH & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_MCH));
            return OP_MCH;
        }
        else if ((OP_CCH & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_CCH));
            return OP_CCH;
        }
        else if ((OP_PB & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_PB));
            return OP_PB;
        }
        return OP_NULL;
    }

    void handleNextOp()
    {
        mOp = getNextOp();
        WLog.d(TAG, "mOp is:" + mOp);
        switch (mOp)
        {
            case OP_PB_SZ:
            case OP_PB:
            case OP_ICH:
            case OP_OCH:
            case OP_MCH:
            case OP_CCH:
            case OP_ICH_ONCE:
            case OP_OCH_ONCE:
            case OP_MCH_ONCE:
            case OP_DEL_PB:
            case OP_DEL_CALLOG_ONLY:
            {
                pullPhoneBook(mOp);
                break;
            }
            default:
                break;
        }
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

    void handleEvt(int evt)
    {
        WLog.d(TAG, "onEvtChange, state is:" + mState + ",evt is:" + evt + ",mDevice:" + mDevice.mName);
        switch (mState)
        {
            case STATE_INITED:
                onEvtChangeStateInited(evt);
                break;
            case STATE_READY:
                onEvtChangeStateReady(evt);
                break;
            case STATE_DOWNLADING:
                onEvtChangeStateDownloading(evt);
                break;
        }
    }

    void gotoState(int state)
    {
        if (state != mState)
        {
            WLog.d(TAG, "gotoState:" + state + ",mDevice:" + mDevice.mName);
            mState = state;
            onEvtChange(EVT_STATE_CHANGED);
        }
    }

    private void onEvtChangeStateInited(int evt)
    {
        switch (evt)
        {
            case EVT_PBAP_SVC_CONNECTED:
            {
                WLog.d(TAG, "onEvtChangeStateInited Enter");
                //confirme to stop last pull
                //	new Thread() {
                //		public void run() {
                //mDevice.finishPullPhonebook();
                WLog.d(TAG, "isCustomPBSyncSwitchOn() = " + isCustomPBSyncSwitchOn());
                if (isCustomPBSyncSwitchOn())
                {
                    if (isSupportPBAutoDownloading())
                    {
                        mOpFlag = OP_PB_SZ | OP_PB | OP_ICH | OP_OCH | OP_MCH;
                    }
                    else
                    {
                        checkIfSyncedPB();
                    }
                }
                else
                {
                    mOpFlag = OP_NULL;
                }
                gotoState(STATE_READY);
                //		}
                //}.start();
                break;
            }
            case EVT_PULL_COMPLETED:
                break;
            case EVT_PBAP_SVC_DISCONNECTED:
                break;
            case EVT_STATE_CHANGED:
                break;
            case EVT_PB_SYNC_SETTINGS_CHANGED:
                break;
            default:
                break;
        }
    }

    private void onEvtChangeStateDownloading(int evt)
    {
        switch (evt)
        {
            case EVT_PBAP_SVC_CONNECTED:
                break;
            case EVT_PULL_COMPLETED:
                handlePBPulledCompleted();
                break;
            case EVT_PBAP_SVC_DISCONNECTED:
                initedState();
                break;
            case EVT_STATE_CHANGED:
                break;
            case EVT_PB_SYNC_SETTINGS_CHANGED:
            {
                WLog.d(TAG, "StateDownloading EVT_PB_SYNC_SETTINGS_CHANGED ,mOp is: " + mOp);
                if (!isCustomPBSyncSwitchOn())
                {
                    mPullList = null;
                    setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_IDLE);
                    mOpFlag = OP_DEL_PB;
                    //signalPullCmt();
                    //gotoState(STATE_READY);
                    new Thread()
                    {
                        public void run()
                        {
                            mDevice.finishPullPhonebook();
                        }
                    }.start();
                }
                else
                {
                    new Thread()
                    {
                        public void run()
                        {
                            mDevice.finishPullPhonebook();
                            mOpFlag = OP_DEL_PB | OP_PB_SZ | OP_PB | OP_ICH | OP_OCH | OP_MCH;
                        }
                    }.start();
                }
                break;
            }
            case EVT_CALL_STATE_CHANGE_IDLE:
            {
                mOpFlag = mOpFlag | OP_ICH_ONCE | OP_OCH_ONCE | OP_MCH_ONCE;
                break;
            }
            case EVT_PROCCESS_TIMEOUT:
            {
                WLog.d(TAG, "Operation timeOut!!!,mOp is:" + mOp);
                signalPullCmt();
                gotoState(STATE_READY);
                break;
            }
            case EVT_PULLPB_BY_MANUAL:
            {
                mContactSyncProccess = 0;
                setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_START);
                mOpFlag = OP_DEL_PB | OP_PB_SZ | OP_PB | OP_ICH | OP_OCH | OP_MCH;
                break;
            }
            case EVT_PULLCALLLOG_ONLY:
            {
                mOpFlag = OP_DEL_CALLOG_ONLY | OP_ICH | OP_OCH | OP_MCH;
                break;
            }
            default:
                break;
        }
    }

    private void onEvtChangeStateReady(int evt)
    {
        switch (evt)
        {
            case EVT_PBAP_SVC_CONNECTED:
                break;
            case EVT_PULL_COMPLETED:
                break;
            case EVT_PBAP_SVC_DISCONNECTED:
            {
                initedState();
                break;
            }
            case EVT_STATE_CHANGED:
            {
                handleNextOp();
                break;
            }
            case EVT_PB_SYNC_SETTINGS_CHANGED:
            {
                if (!isCustomPBSyncSwitchOn())
                {
                    setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_IDLE);
                    mOpFlag = OP_DEL_PB;
                    handleNextOp();
                }
                else
                {
                    mOpFlag = OP_DEL_PB | OP_PB_SZ | OP_PB | OP_ICH | OP_OCH | OP_MCH;
                    handleNextOp();
                }
                break;
            }
            case EVT_CALL_STATE_CHANGE_IDLE:
            {
                mOpFlag = mOpFlag | OP_ICH_ONCE | OP_OCH_ONCE | OP_MCH_ONCE;
                mOp = getNextOp();
                pullPhoneBook(mOp);
                break;
            }
            case EVT_PULLPB_BY_MANUAL:
            {
                mContactSyncProccess = 0;
                setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_START);
                mOpFlag = OP_DEL_PB | OP_PB_SZ | OP_PB | OP_ICH | OP_OCH | OP_MCH;
                handleNextOp();
                break;
            }
            case EVT_PULLCALLLOG_ONLY:
            {
                mOpFlag = OP_DEL_CALLOG_ONLY | OP_ICH | OP_OCH | OP_MCH;
                handleNextOp();
                break;
            }
            default:
                break;
        }
    }

    void initedState()
    {
        WLog.d(TAG, "initedState state:" + STATE_INITED + ",mDevice:" + mDevice.mName);
        mState = STATE_INITED;
        mContactSyncProccess = 0;
        setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_IDLE);
        signalPullCmt();
        mPullPBOffSet = 0;
        mPbSz = 0;
        mOpFlag = OP_NULL;
        new Thread()
        {
            public void run()
            {
                if (isSupportPBAutoDownloading())
                {
                    tryDeleteContactsDb();
                }
                tryDeleteCallLogs(0);
            }
        }.start();
    }

    void waitPullCmt()
    {
        synchronized (mSyncLock)
        {
            mPullComplete = false;
            while (mPullComplete == false)
            {
                try
                {
                    mSyncLock.wait(5000);
                }
                catch (InterruptedException e)
                {
                    // do nothing, go back and wait until the request is
                    // complete
                }
            }
        }
    }

    void waitPullCmt(int timeOut)
    {
        WLog.d(TAG, "waitPullCmt ++++++++");
        synchronized (mSyncLock)
        {
            mPullComplete = false;
            while (mPullComplete == false && timeOut > 0)
            {
                try
                {
                    mSyncLock.wait(1000);
                }
                catch (InterruptedException e)
                {
                }
                timeOut--;
            }
        }
        if (timeOut <= 0 && mPullComplete == false)
        {
            WLog.d(TAG, "TimeOut mOp is:" + mOp);
            onEvtChange(EVT_PROCCESS_TIMEOUT);
        }
    }


    void signalPullCmt()
    {
        WLog.d(TAG, "signalPullCmt -----");
        synchronized (mSyncLock)
        {
            mPullComplete = true;
            mSyncLock.notifyAll();
        }
    }

    public static boolean isPhoneNumber(String number)
    {
        if (TextUtils.isEmpty(number))
        {
            return false;
        }

        Matcher match = Patterns.PHONE.matcher(number);
        return match.matches();
    }


    void pullPhoneBook(final int op)
    {
        gotoState(STATE_DOWNLADING);
            WLog.d(TAG, "pullPhoneBook Enter");
        new Thread()
        {
            public void run()
            {
                WLog.d(TAG, "pullPhoneBook Thread:" + op);
                switch (op)
                {
                    case OP_PB_SZ:
                    {
                        WLog.d(TAG, "Jon: [BT] pullPhoneBook PB sz");
                        mPbSz = 0;
                        mPullPBOffSet = 0;
                        tryDeleteContactsDb();
                        startPBDLProcess();
                        setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_START);
                        WLog.d(TAG, "Jon: [BT] Start Sync pb,mPbSz:" + mPbSz + ",mPullPBOffSet:" + mPullPBOffSet);
                        mDevice.pullPhoneBook(STORAGE_LOCAL, FOLD_PB, 0, 0);
                        waitPullCmt(MAX_OP_TIMEOUT);
                        break;
                    }
                    case OP_PB:
                    {
                        WLog.d(TAG, "pullPhoneBook PB PULL");
                        if (mPbSz > 0 && (mPhoneBookSycedStatus == STATE_CONTACT_SYNC_START || mPhoneBookSycedStatus == STATE_CONTACT_SYNC_VISIBLE))
                        {
                            mDevice.pullPhoneBook(STORAGE_LOCAL, FOLD_PB, MAX_PB_PULL_ITEMS, mPullPBOffSet);
                            mPullPBOffSet += MAX_PB_PULL_ITEMS;
                            if (mContactSyncProccess < 100)
                            {
                                if (mPullPBOffSet <= mPullPBOffSet)
                                {
                                    mContactSyncProccess = mPullPBOffSet * 100 / mPbSz;
                                    WLog.d(TAG, "Jon: [BT] sync mContactSyncProccess is:" + mContactSyncProccess);
                                }
                            }
                            if (mContactSyncProccess > 100)
                            {
                                mContactSyncProccess = 100;
                            }
                            waitPullCmt(MAX_OP_TIMEOUT);
                            //setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_VISIBLE);
                        }
                        else
                        {
                            WLog.d(TAG, "Jon: [BT] Finish Sync pb,mPbSz:" + mPbSz + ",mPullPBOffSet:" + mPullPBOffSet +
                                    "mPhoneBookSycedStatus is:" + mPhoneBookSycedStatus);
                            endPBDLProccess();
                            setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_FINISHED);
                            gotoState(STATE_READY);
                        }
                        break;
                    }
                    case OP_ICH:
                    {
                        WLog.d(TAG, "pullPhoneBook ICH PULL");
                        tryDeleteCallLogs(CallLog.Calls.INCOMING_TYPE);
                        mDevice.pullPhoneBook(STORAGE_LOCAL, FOLD_ICH, MAX_CALLLOG_ITEMS, 0);
                        waitPullCmt(MAX_OP_TIMEOUT);
                        break;
                    }
                    case OP_OCH:
                    {
                        WLog.d(TAG, "pullPhoneBook OCH PULL");
                        tryDeleteCallLogs(CallLog.Calls.OUTGOING_TYPE);
                        mDevice.pullPhoneBook(STORAGE_LOCAL, FOLD_OCH, MAX_CALLLOG_ITEMS, 0);
                        waitPullCmt(MAX_OP_TIMEOUT);
                        break;
                    }
                    case OP_MCH:
                    {
                        WLog.d(TAG, "pullPhoneBook MCH PULL");
                        tryDeleteCallLogs(CallLog.Calls.MISSED_TYPE);
                        mDevice.pullPhoneBook(STORAGE_LOCAL, FOLD_MCH, MAX_CALLLOG_ITEMS, 0);
                        waitPullCmt(MAX_OP_TIMEOUT);
                        break;
                    }
                    case OP_DEL_PB:
                    {
                        new Thread()
                        {
                            public void run()
                            {
                                tryDeleteContactsDb();
                                tryDeleteCallLogs(0);
                                signalPullCmt();
                                gotoState(STATE_READY);
                            }
                        }.start();
                        waitPullCmt(MAX_OP_TIMEOUT);
                        break;
                    }
                    case OP_DEL_CALLOG_ONLY:
                    {
                        new Thread()
                        {
                            public void run()
                            {
                                tryDeleteCallLogs(0);
                                signalPullCmt();
                                gotoState(STATE_READY);
                            }
                        }.start();
                        waitPullCmt(MAX_OP_TIMEOUT);
                        break;
                    }
                    case OP_ICH_ONCE:
                    {
                        WLog.d(TAG, "pullPhoneBook ICH PULL ONCE");
                        mDevice.pullPhoneBook(STORAGE_LOCAL, FOLD_ICH, 1, 0);
                        waitPullCmt(MAX_OP_TIMEOUT);
                        break;
                    }
                    case OP_OCH_ONCE:
                    {
                        WLog.d(TAG, "pullPhoneBook OCH PULL ONCE");
                        mDevice.pullPhoneBook(STORAGE_LOCAL, FOLD_OCH, 1, 0);
                        waitPullCmt(MAX_OP_TIMEOUT);
                        break;
                    }
                    case OP_MCH_ONCE:
                    {
                        WLog.d(TAG, "pullPhoneBook MCH PULL ONCE");
                        mDevice.pullPhoneBook(STORAGE_LOCAL, FOLD_MCH, 1, 0);
                        waitPullCmt(MAX_OP_TIMEOUT);
                        break;
                    }

                    default:
                        break;
                }

            }

        }.start();
    }

    void tryDeleteContactsDb()
    {
        WLog.d(TAG, "Jon: deleteContacts Enter");
        setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_IDLE);
        Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(RawContacts.ACCOUNT_NAME, mAccount.name)
                .appendQueryParameter(RawContacts.ACCOUNT_TYPE, mAccount.type).build();

        try
        {
            mContext.getContentResolver().delete(rawContactUri, null, null);
        }
        catch (Exception e)
        {
            WLog.d(TAG, "Jon: [BT] Exception:" + e);
        }
    }

    void tryInsert2ContactsDb()
    {
        WLog.d(TAG, "tryInsert2ContactsDb");
        if (mPullList != null)
        {
            ContentResolver resolver = mContext.getContentResolver();
            Iterator<BtPBContact> iterator = mPullList.iterator();
            ArrayList<ContentProviderOperation> pbOperations = new ArrayList<ContentProviderOperation>();
            int rawContactInsertIndex = 0;
            int index = 0;
            while (mPullList != null && iterator.hasNext())
            {
                if (isCustomPBSyncSwitchOff() || (mState != STATE_DOWNLADING) || (mPhoneBookSycedStatus == STATE_CONTACT_SYNC_IDLE || mPhoneBookSycedStatus == STATE_CONTACT_SYNC_FINISHED))
                {
                    return;
                }
                BtPBContact contact = (BtPBContact) iterator.next();
                if (!isPhoneNumber(contact.mTelNumber))
                {
                    continue;
                }

                // WLog.d(TAG, "Jon: ------------------------------------");
                // WLog.d(TAG, "Jon: " + contact.toString());
                // WLog.d(TAG, "Jon: ------------------------------------");
                rawContactInsertIndex = pbOperations.size();
                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
                builder.withValue(RawContacts.ACCOUNT_NAME, mAccount.name);
                builder.withValue(RawContacts.ACCOUNT_TYPE, mAccount.type);
                builder.withYieldAllowed(true);
                pbOperations.add(builder.build());
                index++;

                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, rawContactInsertIndex);
                builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                builder.withValue(StructuredName.DISPLAY_NAME, contact.mName);
                builder.withYieldAllowed(true);
                pbOperations.add(builder.build());
                index++;

                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Phone.RAW_CONTACT_ID, rawContactInsertIndex);
                builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                builder.withValue(Phone.TYPE, Phone.TYPE_MOBILE);
                builder.withValue(Phone.NUMBER, PhoneNumberUtils.stripSeparators(contact.mTelNumber));
                builder.withValue(Data.IS_PRIMARY, 1);
                builder.withYieldAllowed(true);
                pbOperations.add(builder.build());
                index++;
            }

            try
            {
                if (isCustomPBSyncSwitchOff() || (mState != STATE_DOWNLADING))
                {
                    return;
                }
                mContext.getContentResolver().applyBatch(ContactsContract.AUTHORITY, pbOperations);
            }
            catch (RemoteException e)
            {
                WLog.e(TAG, String.format("Jon: [BT] %s: %s", e.toString(), e.getMessage()));
            }
            catch (OperationApplicationException e)
            {
                WLog.e(TAG, String.format("Jon: [BT] %s: %s", e.toString(), e.getMessage()));
            }
            catch (SQLiteDatabaseCorruptException e)
            {
                WLog.e(TAG, String.format("Jon: [BT] %s: %s", e.toString(), e.getMessage()));
            }
            catch (Exception e)
            {
                WLog.e(TAG, String.format("Jon: [BT] %s: %s", e.toString(), e.getMessage()));
            }
            //			notifyPhoneBookUpdated();
            if (mPhoneBookSycedStatus == STATE_CONTACT_SYNC_START)
            {
                WLog.d(TAG, "Jon: [BT] setPhonebook Visible");
                setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_VISIBLE);
            }
            WLog.d(TAG, "Jon: [BT] Contacts Sync Once Ok");
        }
    }

    void handlePBPulledCompleted()
    {
        WLog.d(TAG, "handlePBPulledCompleted Enter!");
        new Thread()
        {
            public void run()
            {
                switch (mOp)
                {
                    case OP_PB_SZ:
                    {
                        //if (mPullList != null){
                        //	mPbSz = mPullList.get(0).mPbSize;
                        //}
                        WLog.d(TAG, "Jon: [BT] pullPhoneBook mPbSize is:" + mPbSz);
                        mPullList = null;
                        signalPullCmt();
                        if (mState != STATE_INITED)
                        {
                            gotoState(STATE_READY);
                        }
                        return;
                    }
                    case OP_PB:
                    {
                        tryInsert2ContactsDb();

                        if (mPullPBOffSet < mPbSz)
                        {
                            mOpFlag = mOpFlag | OP_PB;
                            WLog.d(TAG, "handlePBPulledCompleted mPullPBOffSet:" + mPullPBOffSet);
                        }
                        else
                        {
                            WLog.d(TAG,
                                    "Jon: [BT] all pull finished Enter,Finish Sync pb,mPbSz:" + mPbSz + ",mPullPBOffSet:" + mPullPBOffSet);
                            endPBDLProccess();
                            setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_FINISHED);
                        }
                        mPullList = null;
                        signalPullCmt();
                        if (mState != STATE_INITED)
                        {
                            gotoState(STATE_READY);
                        }
                        return;
                    }
                    case OP_ICH:
                    case OP_OCH:
                    case OP_MCH:
                    {
                        tryInsert2CallLogDb();
                        mPullList = null;
                        signalPullCmt();
                        if (mState != STATE_INITED)
                        {
                            gotoState(STATE_READY);
                        }
                        return;
                    }
                    case OP_ICH_ONCE:
                    case OP_OCH_ONCE:
                    case OP_MCH_ONCE:
                    {
                        tryInsert2CallLogDbOnce();
                        mPullList = null;
                        signalPullCmt();
                        if (mState != STATE_INITED)
                        {
                            gotoState(STATE_READY);
                        }
                        return;
                    }

                    default:
                        return;
                }
            }
        }.start();
    }

    boolean isCustomPBSyncSwitchOn()
    {
        return (Settings.System
                .getInt(mContext.getContentResolver(), BluetoothSettings.BLUETOOTH_PHONEBOOK_SYNC, 1) == 1);
    }

    boolean isCustomPBSyncSwitchOff()
    {
        return (Settings.System
                .getInt(mContext.getContentResolver(), BluetoothSettings.BLUETOOTH_PHONEBOOK_SYNC, 1) == 0);
    }


    boolean isCustomBtSwitchOn()
    {
        return (Settings.System.getInt(mContext.getContentResolver(), Settings.Global.BLUETOOTH_ON, 0) == 1);
    }

    int getPhoneBookSyncedStatus()
    {
        return mPhoneBookSycedStatus;
    }

    void setPhoneBookSyncedStatus(int status)
    {
        //if(mPhoneBookSycedStatus != status) {
        mPhoneBookSycedStatus = status;
        if (STATE_CONTACT_SYNC_IDLE != status)
        {
            Intent intent = new Intent(BluetoothDevice.ACTION_PHONEBOOK_SYNC_STATUS);
            intent.putExtra(BluetoothDevice.EXTRA_PB_SYNC_STATE, mPhoneBookSycedStatus);
            mContext.sendBroadcast(intent);
        }
        //}

    }

    void getPhoneBookByManual()
    {
        WLog.d(TAG, "getPhoneBookByManual Enter");
        onEvtChange(EVT_PULLPB_BY_MANUAL);
    }

    int getPhoneBookSyncProgress()
    {
        WLog.d(TAG, "getPhoneBookSyncProgress,mContactSyncProccess is:" + mContactSyncProccess);
        return mContactSyncProccess;
    }

    void startPBDLProcess()
    {
        mContactSyncProccess = 0;
        notifyiflytekPhoneBookSynced(true);
    }

    void endPBDLProccess()
    {
        mContactSyncProccess = 100;
        notifyiflytekPhoneBookSynced(false);
    }

    void notifyiflytekPhoneBookSynced(boolean start)
    {
        if (start)
        {
            Intent intent = new Intent("com.chleon.sync.contacts.start");
            mContext.sendBroadcast(intent);
        }
        else
        {
            Intent intent = new Intent("com.chleon.sync.contacts.completed");
            mContext.sendBroadcast(intent);
        }
    }

    void onPBSycSettingsChanged()
    {
        onEvtChange(EVT_PB_SYNC_SETTINGS_CHANGED);
    }

    void onCallStateChanged(int state)
    {
        if (state == BluetoothDevice.CALL_STATE_IDLE && mState > STATE_INITED)
        {
            onEvtChange(EVT_CALL_STATE_CHANGE_IDLE);
        }
    }

    int convert2CallLogType()
    {
        switch (mOp)
        {
            case OP_ICH://received
            case OP_ICH_ONCE:
                return CallLog.Calls.INCOMING_TYPE;
            case OP_OCH://outgoing
            case OP_OCH_ONCE:
                return CallLog.Calls.OUTGOING_TYPE;
            case OP_MCH://missed
            case OP_MCH_ONCE:
                return CallLog.Calls.MISSED_TYPE;
            default:
                return -1;
        }
    }

    long convert2CallLogDate(String BtCallTime)
    {
        if (BtCallTime == null || BtCallTime.length() == 0)
        {
            return -1;
        }
        try
        {
            String time = BtCallTime.replace("T", "");
            return Long.parseLong(time);
        }
        catch (Exception e)
        {
            return -1;
        }
    }


    private void tryDeleteCallLogs(int btCallType)
    {
        if (btCallType == 0)
        {
            WLog.d(TAG, "Jon:  delete all call log type");
            try
            {
                mContext.getContentResolver().delete(CallLog.Calls.CONTENT_URI, null, null);
            }
            catch (Exception e)
            {
                WLog.d(TAG, "Jon: [BT] deleteCallLogs:" + btCallType);
            }
        }
        else
        {
            WLog.d(TAG, "Jon:  delete all call log type:" + btCallType);
            try
            {
                mContext.getContentResolver().delete(CallLog.Calls.CONTENT_URI, CallLog.Calls.TYPE + "=?",
                        new String[]{String.valueOf(btCallType)});
            }
            catch (Exception e)
            {
                WLog.d(TAG, "Jon: [BT] deleteCallLogs:" + btCallType);
            }
        }
    }

    boolean exsitInCalllogDB(BtPBContact calllog)
    {
        final Cursor cursor = mContext.getContentResolver()
                .query(CallLog.Calls.CONTENT_URI, null, CallLog.Calls.NUMBER + "=? AND " +
                        CallLog.Calls.CACHED_NAME + "=? AND " +
                        CallLog.Calls.DATE + "=?", new String[]{calllog.mTelNumber, calllog.mName, String.valueOf(
                        convert2CallLogDate(calllog.mCallLogTime))}, CallLog.Calls.DEFAULT_SORT_ORDER);
        if (cursor != null && cursor.getCount() > 0)
        {
            cursor.close();
            return true;
        }
        return false;
    }

    void notifyCallLogUpdated()
    {
        Intent intent = new Intent("com.chleon.calllog.updated");
        mContext.sendBroadcast(intent);
        WLog.d(TAG, "Call WLog updated");
    }


    //   void notifyPhoneBookUpdated() {
    //       Intent intent = new Intent("com.chleon.phonebook.updated");
    //       mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    //       WLog.d(TAG,"PhoneBook updated");
    //   }


    void tryInsert2CallLogDbOnce()
    {
        WLog.d(TAG, "JonCALLLOG:  tryInsert2CallLogDbOnce Enter");
        if (mPullList != null)
        {
            ContentResolver resolver = mContext.getContentResolver();
            Iterator<BtPBContact> iterator = mPullList.iterator();
            ArrayList<ContentProviderOperation> calllogOps = new ArrayList<ContentProviderOperation>();

            while (mPullList != null && iterator.hasNext())
            {
                if (isCustomPBSyncSwitchOff() || (mState != STATE_DOWNLADING))
                {
                    return;
                }

                BtPBContact calllog = (BtPBContact) iterator.next();
                WLog.d(TAG, "JonCALLLOG: ------------------------------------");
                WLog.d(TAG, "JonCALLLOG: " + calllog.toString());
                WLog.d(TAG, "JonCALLLOG: ------------------------------------");

                if (calllog.mCallLogTime == null)
                {
                    continue;
                }
                if (!exsitInCalllogDB(calllog))
                {
                    ContentProviderOperation.Builder builder = ContentProviderOperation
                            .newInsert(CallLog.Calls.CONTENT_URI);

                    builder.withValue(CallLog.Calls.TYPE, convert2CallLogType());
                    builder.withValue(CallLog.Calls.NUMBER, calllog.mTelNumber);
                    builder.withValue(CallLog.Calls.CACHED_NAME, calllog.mName);
                    builder.withValue(CallLog.Calls.DATE, convert2CallLogDate(calllog.mCallLogTime));
                    builder.withValue(CallLog.Calls.NEW, Integer.valueOf(1));
                    builder.withYieldAllowed(true);
                    calllogOps.add(builder.build());
                }
            }
            try
            {
                if (isCustomPBSyncSwitchOff() || (mState != STATE_DOWNLADING))
                {
                    return;
                }
                if (calllogOps.size() > 0)
                {
                    mContext.getContentResolver().applyBatch(CallLog.AUTHORITY, calllogOps);
                }
            }
            catch (RemoteException e)
            {
                WLog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
            catch (OperationApplicationException e)
            {
                WLog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
            catch (SQLiteDatabaseCorruptException e)
            {
                WLog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
            catch (IllegalStateException e)
            {
                WLog.e(TAG, String.format("Jon: [BT] %s: %s", e.toString(), e.getMessage()));
            }
            notifyCallLogUpdated();
            WLog.d(TAG, "JonCALLLOG:  getCallLog insertCallLogs Exit");

        }

    }

    void tryInsert2CallLogDb()
    {
        if (mPullList != null)
        {
            WLog.d(TAG, "JonCALLLOG:  getCallLog insertCallLogs Enter");
            ContentResolver resolver = mContext.getContentResolver();
            Iterator<BtPBContact> iterator = mPullList.iterator();
            ArrayList<ContentProviderOperation> calllogOps = new ArrayList<ContentProviderOperation>();

            while (mPullList != null && iterator.hasNext())
            {
                if (isCustomPBSyncSwitchOff() || (mState != STATE_DOWNLADING))
                {
                    return;
                }

                BtPBContact calllog = (BtPBContact) iterator.next();
                WLog.d(TAG, "JonCALLLOG: ------------------------------------");
                WLog.d(TAG, "JonCALLLOG: " + calllog.toString());
                WLog.d(TAG, "JonCALLLOG: ------------------------------------");

                if (calllog.mCallLogTime == null)
                {
                    continue;
                }

                ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newInsert(CallLog.Calls.CONTENT_URI);

                builder.withValue(CallLog.Calls.TYPE, convert2CallLogType());
                builder.withValue(CallLog.Calls.NUMBER, calllog.mTelNumber);
                builder.withValue(CallLog.Calls.CACHED_NAME, calllog.mName);
                builder.withValue(CallLog.Calls.DATE, convert2CallLogDate(calllog.mCallLogTime));
                builder.withValue(CallLog.Calls.NEW, Integer.valueOf(1));
                builder.withYieldAllowed(true);
                calllogOps.add(builder.build());
            }
            try
            {
                if (isCustomPBSyncSwitchOff() || (mState != STATE_DOWNLADING))
                {
                    return;
                }
                mContext.getContentResolver().applyBatch(CallLog.AUTHORITY, calllogOps);
            }
            catch (RemoteException e)
            {
                WLog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
            catch (OperationApplicationException e)
            {
                WLog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
            catch (SQLiteDatabaseCorruptException e)
            {
                WLog.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
            catch (IllegalStateException e)
            {
                WLog.e(TAG, String.format("Jon: [BT] %s: %s", e.toString(), e.getMessage()));
            }
            notifyCallLogUpdated();
            WLog.d(TAG, "JonCALLLOG:  getCallLog insertCallLogs Exit");
        }
    }


    private boolean isSupportPBAutoDownloading()
    {
        return true;
    }

    private final class ContactsQueryHandler extends AsyncQueryHandler
    {
        public ContactsQueryHandler(ContentResolver cr)
        {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor)
        {
            boolean hasAllReadySyned = false;
            super.onQueryComplete(token, cookie, cursor);
            if (cursor != null)
            {
                if (cursor.getCount() > 0)
                {
                    hasAllReadySyned = true;
                }
                cursor.close();
            }
            if (hasAllReadySyned)
            {
                endPBDLProccess();
                setPhoneBookSyncedStatus(STATE_CONTACT_SYNC_FINISHED);
                onEvtChange(EVT_PULLCALLLOG_ONLY);
            }
            else
            {
                //not syned ,
                onEvtChange(EVT_PULLPB_BY_MANUAL);
            }
        }
    }


    void checkIfSyncedPB()
    {
        if (mDevice != null)
        {
            mContactsAsyncQueryHandler
                    .startQuery(0, null, RawContacts.CONTENT_URI, new String[]{"_id", "_id", "display_name", "version"},
                            "version!= 1 AND account_type=? AND account_name=?",
                            new String[]{"com.chleon.bluetooth.sync", mDevice.mAddress}, "sort_key");
        }

    }
}

