package com.service.bluetooth;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class BtMessages
{
    final String TAG = "Jon: BtMessages";

    public static final int MAX_WAIT_TIMEOUT = 10;

    public static final String BEGIN_BMSG = "BEGIN:BMSG\r\n";
    public static final String END_BMSG = "END:BMSG\r\n";
    public static final String VERSION = "VERSION:";
    public static final String TYPE = "TYPE:";
    public static final String READSTATUS = "STATUS:";
    public static final String FOLDER = "FOLDER:";
    public static final String BEGIN_BENV = "BEGIN:BENV\r\n";
    public static final String END_BENV = "END:BENV\r\n";
    public static final String BEGIN_BBODY = "BEGIN:BBODY\r\n";
    public static final String END_BBODY = "END:BBODY\r\n";
    public static final String PARTID = "PARTID:";
    public static final String ENCODING = "ENCODING:";
    public static final String CHARSET = "CHARSET:";
    public static final String LANGUAGE = "LANGUAGE:";
    public static final String LENGTH = "LENGTH:";
    public static final String BEGIN_MSG = "BEGIN:MSG\r\n";
    public static final String END_MSG = "END:MSG\r\n";
    public static final String READ = "READ\r\n";
    public static final String UNREAD = "UNREAD\r\n";
    public static final String EMAIL = "EMAIL\r\n";
    public static final String SMS_GSM = "SMS_GSM\r\n";
    public static final String SMS_CDMA = "SMS_CDMA\r\n";
    public static final String MMS = "MMS\r\n";
    public static final String RETURN = "\r\n";
    public static final String BEGIN_VCARD = "BEGIN:VCARD\r\n";
    public static final String VCARD_VERSION = "VERSION:2.1\r\n";
    public static final String VCARD_NAME = "N:";
    public static final String VCARD_TEL = "TEL:";
    public static final String END_VCARD = "END:VCARD\r\n";


    final static int EVT_MAP_SVC_CONNECTED = 0;
    final static int EVT_MAP_SVC_DISCONNECTED = 1;
    final static int EVT_STATE_CHANGED = 2;
    final static int EVT_MORE_MSG_LIST_DATA_IND = 3;
    final static int EVT_GET_MSG_DATA_CMT = 4;
    final static int EVT_GET_MSG = 5;
    final static int EVT_SEND_MSG = 6;
    final static int EVT_GET_MSG_DATA_DONE = 7;
    final static int EVT_WAIT_TIMEOUT = 8;
    final static int EVT_PUSH_MSG_DATA_CMT = 9;
    final static int EVT_MSG_EVT = 10;
    final static int EVT_PUSH_MSG_DATA_CONT = 11;
    final static int EVT_DISCONNECT_SVC = 12;

    private static final int MSG_EVT_TYPE_NEW_MSG = 0;
    private static final int MSG_EVT_TYPE_MSG_DEL = 1;
    private static final int MSG_EVT_TYPE_MSG_SENT_PHONE = 2;
    private static final int MSG_EVT_TYPE_MSG_SENT_NET = 3;
    private static final int MSG_EVT_TYPE_MSG_DELIVED = 4;
    private static final int MSG_EVT_TYPE_MSG_SHIFTED = 5;
    private static final int MSG_EVT_TYPE_MSG_MEMORY = 6;


    Context mContext;
    BtDeviceRemote mDevice;
    int mCurrentFold = -1;

    final static int STATE_INITED = 0;
    final static int STATE_READY = 1;
    final static int STATE_DOWNLADING = 2;

    int mState;

    final static int STATUS_SYNC_INIT = 0;
    final static int STATE_SYNC_START = 1;
    final static int STATUS_SYNC_READY = 3;

    int mSyncMLStatus = STATUS_SYNC_INIT;

    MyHandler mHandler;

    static final int OP_NULL = 0x00;
    static final int OP_SEND_MSG = 0x01;
    static final int OP_PULL_ML_CMT = 0x02;
    static final int OP_PULL_ML = 0x04;
    static final int OP_PULL_ML_CONT = 0x08;
    static final int OP_GET_MSG = 0x10;
    static final int OP_DISCONNECT_SVC = 0x20;
    static final int OP_PULL_ML_CMT_EARLY = 0x40;


    int mOp = OP_NULL;
    int mOpFlag = OP_NULL;


    final Object mSyncLock = new Object();
    boolean mPullComplete = true;

    static final int FOLD_INBOX = 0;
    static final int FOLD_OUTBOX = 1;
    static final int FOLD_SENT = 2;
    static final int FOLD_DELETE = 4;
    static final int FOLD_DRAFT = 5;

    int mPullFoldFlag = 0;//(1 << FOLD_INBOX) | (1<< FOLD_SENT);

    static final int MAX_PULL_MSGS_IN_FOLD = 5;

    int needMore = 0;
    StringBuffer mListPackets = null;
    ArrayList<BluetoothMessage> mMsgList = null;

    String mRetriveMsgHandle = null;
    String mRetriveMsg = null;

    static final int MSG_EMAIL = 0;
    static final int MSG_GSM = 1;
    static final int MSG_CDMA = 2;
    static final int MSG_MMS = 3;

    static final int SENDMSG_REASON_SUCCESS = 0;
    static final int SENDMSG_REASON_TIMEOUT = 1;
    static final int SENDMSG_REASON_CREATE_NEW_MESSAGE_FAILED = 2;
    static final int SENDMSG_REASON_INVALID_MSG_REQ = 3;
    static final int SENDMSG_REASON_CREATE_NEW_MESSAGE_RET_FAILED = 4;
    static final int SENDMSG_REASON_FILL_MSG_FAILED = 5;
    static final int SENDMSG_REASON_SEND_MESSAGE_FAILED = 6;
    static final int SENDMSG_REASON_NO_SEND_FILEID = 7;
    static final int SENDMSG_REASON_SEND_FAILED = 8;
    static final int SENDMSG_REASON_SENDTO_PHONE = 9;


    //BluetoothMessage mSendMsg = null;
    class SendMsg
    {
        int more;
        String data;
    }

    ArrayList<SendMsg> mSendMsgBuf = new ArrayList<SendMsg>();
    static final int MAX_SEND_MSG_LEN = 100;

    public BtMessages(BtDeviceRemote device)
    {
        mDevice = device;
        mContext = mDevice.mService.mContext;
        mHandler = new MyHandler(mDevice.mService.mBtMainThread.getLooper());
        initedState();
    }

    public void onMAPSvcDisconnecte()
    {
        WLog.d(TAG, "onPBAPSvcDisconnecte Enter");
        onEvtChange(EVT_MAP_SVC_DISCONNECTED);
    }

    public void onMAPSvcConnected()
    {
        WLog.d(TAG, "onPBAPSvcConnected Enter");
        onEvtChange(EVT_MAP_SVC_CONNECTED);
    }

    public void onMsgListDataIndChanged(int more, String packet)
    {
        WLog.d(TAG, "onMsgListDataIndChanged Enter");
        needMore = more;
        if (mListPackets == null)
        {
            mListPackets = new StringBuffer();
        }
        mListPackets.append(packet);
        //		WLog.d(TAG,"received Msg Packet String:"+packet);
        //	    try {
        //            WLog.d(TAG,"received Msg Packet Hex:"+HexDump.toHexString(packet.getBytes("UTF-8")));
        //       } catch (UnsupportedEncodingException uee) {
        //            WLog.e(TAG, "UTF-8 not supported?!?");  // this should not happen
        //        }
        onEvtChange(EVT_MORE_MSG_LIST_DATA_IND);
    }

    public void onMsgDataIndChanged(int more, String packet)
    {
        mRetriveMsg = packet;
        onEvtChange(EVT_GET_MSG_DATA_DONE);
    }


    public void onGetMsgDataCmt()
    {
        onEvtChange(EVT_GET_MSG_DATA_CMT);
    }

    public void onPushMsgDataCmt()
    {
        onEvtChange(EVT_PUSH_MSG_DATA_CMT);
    }


    public void onPushMsgDataCont()
    {
        onEvtChange(EVT_PUSH_MSG_DATA_CONT);
    }

    public void onPushMsgEvt(String evt)
    {
        if ("DeliverySuccess".equals(evt))
        {
            WLog.d(TAG, "onPushMsgEvt:DeliverySuccess");
            notifyMessagesEvtChanged(MSG_EVT_TYPE_MSG_SENT_NET);
            //onSendTransactionEnd(SENDMSG_REASON_SUCCESS);
            onEvtChange(EVT_MSG_EVT);
        }
        else if ("SendingSuccess".equals(evt))
        {
            WLog.d(TAG, "onPushMsgEvt:SendingSuccess");
            notifyMessagesEvtChanged(MSG_EVT_TYPE_MSG_SENT_PHONE);
            //onSendTransactionEnd(SENDMSG_REASON_SENDTO_PHONE);
            onEvtChange(EVT_MSG_EVT);
        }
        else if ("NewMessage".equals(evt))
        {
            WLog.d(TAG, "onPushMsgEvt:NewMessage");
            notifyMessagesEvtChanged(MSG_EVT_TYPE_NEW_MSG);
            onEvtChange(EVT_MSG_EVT);
        }
        //onEvtChange(EVT_MSG_EVT);
    }


    void onSendTransactionEnd(int reaseon)
    {
        WLog.d(TAG, "onSendTransactionEnd, reason: " + reaseon);
        Intent intent = new Intent("chleon.android.bluetooth.device.message.send");
        intent.putExtra("REASON", reaseon);
        mContext.sendBroadcast(intent);
    }


    int getNextOp()
    {
        if ((OP_SEND_MSG & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_SEND_MSG));
            return OP_SEND_MSG;
        }
        else if ((OP_PULL_ML_CMT_EARLY & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_PULL_ML_CMT_EARLY));
            return OP_PULL_ML_CMT_EARLY;
        }
        else if ((OP_PULL_ML & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_PULL_ML));
            return OP_PULL_ML;
        }
        else if ((OP_PULL_ML_CONT & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_PULL_ML_CONT));
            return OP_PULL_ML_CONT;
        }
        else if ((OP_PULL_ML_CMT & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_PULL_ML_CMT));
            return OP_PULL_ML_CMT;
        }
        else if ((OP_DISCONNECT_SVC & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_DISCONNECT_SVC));
            return OP_DISCONNECT_SVC;
        }
        else if ((OP_GET_MSG & mOpFlag) > 0)
        {
            mOpFlag = mOpFlag & (~(OP_GET_MSG));
            return OP_GET_MSG;
        }
        return OP_NULL;
    }

    void handleNextOp()
    {
        mOp = getNextOp();
        WLog.d(TAG, "mOp is:" + mOp);
        switch (mOp)
        {
            case OP_SEND_MSG:
            {
                handleOperation(OP_SEND_MSG);
                break;
            }
            case OP_PULL_ML:
            {
                handleOperation(OP_PULL_ML);
                break;
            }
            case OP_PULL_ML_CONT:
            {
                handleOperation(OP_PULL_ML_CONT);
                break;
            }
            case OP_PULL_ML_CMT:
            {
                handleOperation(OP_PULL_ML_CMT);
                break;
            }
            case OP_GET_MSG:
            {
                handleOperation(OP_GET_MSG);
                break;
            }
            case OP_DISCONNECT_SVC:
            {
                handleOperation(OP_DISCONNECT_SVC);
                break;
            }
            case OP_PULL_ML_CMT_EARLY:
            {
                handleOperation(OP_PULL_ML_CMT_EARLY);
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
        WLog.d(TAG, "onEvtChange, state is:" + mState + ",evt is:" + evt);
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
            mState = state;
            onEvtChange(EVT_STATE_CHANGED);
        }
    }

    void initedState()
    {
        mState = STATE_INITED;
        setMLSyncedStatus(STATUS_SYNC_INIT);
        mOp = OP_NULL;
        mOpFlag = OP_NULL;
        mPullFoldFlag = 0;
        mListPackets = null;
        mMsgList = null;
        mRetriveMsgHandle = null;
        mCurrentFold = -1;
        mSendMsgBuf.clear();
        signalPullCmt();
    }

    void onWaitTimeOut()
    {
        WLog.d(TAG, "onWaitTimeOut");
        signalPullCmt();
        if (mDevice.getMAPSvcState() == BtDeviceRemote.MAP_CON_STATE_SVC_SUCCESS)
        {
            gotoState(STATE_READY);
        }
        else
        {
            initedState();
        }
    }

    void setMLSyncedStatus(int status)
    {
        mSyncMLStatus = status;
        if (STATUS_SYNC_INIT != status)
        {
            Intent intent = new Intent(BluetoothDevice.ACTION_MESSAGES_SYNC_STATUS);
            intent.putExtra(BluetoothDevice.EXTRA_MSG_SYNC_STATE, mSyncMLStatus);
            mContext.sendBroadcast(intent);
        }
    }

    private void onEvtChangeStateInited(int evt)
    {
        switch (evt)
        {
            case EVT_MAP_SVC_CONNECTED:
            {
                WLog.d(TAG, "onEvtChangeStateInited Enter");
                mOpFlag = OP_PULL_ML;
                mPullFoldFlag = (1 << FOLD_INBOX) | (1 << FOLD_SENT);
                gotoState(STATE_READY);
                break;
            }
            case EVT_DISCONNECT_SVC:
            {
                new Thread()
                {
                    public void run()
                    {
                        WLog.d(TAG, "Jon: [BT] disconnnect MAP SVC CP 1");
                        mDevice.mService.deviceConnect(mDevice.mAddress, BTConstants.BT_MAP, false);
                    }
                }.start();
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
            case EVT_STATE_CHANGED:
            {
                handleNextOp();
                break;
            }
            case EVT_GET_MSG:
            {
                mOpFlag = mOpFlag | OP_GET_MSG;
                handleNextOp();
                break;
            }
            case EVT_SEND_MSG:
                mOpFlag = mOpFlag | OP_SEND_MSG;
                handleNextOp();
                break;
            case EVT_MAP_SVC_DISCONNECTED:
                initedState();
                break;
            case EVT_MSG_EVT:
                mOpFlag = mOpFlag | OP_PULL_ML_CMT_EARLY | OP_PULL_ML | OP_PULL_ML_CMT;
                mPullFoldFlag = (1 << FOLD_INBOX) | (1 << FOLD_SENT);
                mListPackets = null;
                mMsgList = null;
                handleNextOp();
                break;
            case EVT_DISCONNECT_SVC:
            {
                mOpFlag = mOpFlag | OP_DISCONNECT_SVC;
                handleNextOp();
                break;
            }
            default:
                break;
        }
    }

    private void onEvtChangeStateDownloading(int evt)
    {
        switch (evt)
        {
            case EVT_MORE_MSG_LIST_DATA_IND:
            {
                handleListMoreDataIndication();
                break;
            }
            case EVT_GET_MSG_DATA_CMT:
            {
                handleMessageOperationComplete();
                break;
            }
            case EVT_GET_MSG:
            {
                mOpFlag = mOpFlag | OP_GET_MSG;
                break;
            }
            case EVT_SEND_MSG:
                mOpFlag = mOpFlag | OP_SEND_MSG;
                break;
            case EVT_MAP_SVC_DISCONNECTED:
                initedState();
                break;
            case EVT_GET_MSG_DATA_DONE:
                handleGetMessageDataDone();
                break;
            case EVT_WAIT_TIMEOUT:
                onWaitTimeOut();
                break;
            case EVT_PUSH_MSG_DATA_CMT:
                handlePushMessageDataComplete();
                break;
            case EVT_PUSH_MSG_DATA_CONT:
                handlePushMessageDataContinue();
                break;
            case EVT_MSG_EVT:
                mOpFlag = mOpFlag | OP_PULL_ML_CMT_EARLY | OP_PULL_ML | OP_PULL_ML_CMT;
                mPullFoldFlag = (1 << FOLD_INBOX) | (1 << FOLD_SENT);
                mListPackets = null;
                mMsgList = null;
                break;
            case EVT_DISCONNECT_SVC:
            {
                mOpFlag = mOpFlag | OP_DISCONNECT_SVC;
                break;
            }
            default:
                break;
        }
    }

    void waitPullCmt()
    {
        synchronized (mSyncLock)
        {
            mPullComplete = false;
            int timeOut = 0;
            while (mPullComplete == false && timeOut < MAX_WAIT_TIMEOUT)
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
                timeOut++;
            }
            if (mPullComplete == false)
            {
                WLog.d(TAG, "waitCmd complete timeout!!!!!");
                onEvtChange(EVT_WAIT_TIMEOUT);
            }
        }
    }

    void waitPullCmt(int seconds)
    {
        synchronized (mSyncLock)
        {
            mPullComplete = false;
            int timeOut = 0;
            while (mPullComplete == false && timeOut < seconds)
            {
                try
                {
                    mSyncLock.wait(1000);
                }
                catch (InterruptedException e)
                {
                    // do nothing, go back and wait until the request is
                    // complete
                }
                timeOut++;
            }
            if (mPullComplete == false)
            {
                WLog.d(TAG, "waitCmd complete timeout!!!!!");
                onEvtChange(EVT_WAIT_TIMEOUT);
            }
        }
    }


    void signalPullCmt()
    {
        synchronized (mSyncLock)
        {
            mPullComplete = true;
            mSyncLock.notifyAll();
        }
    }

    void handleOperation(final int op)
    {
        gotoState(STATE_DOWNLADING);
        WLog.d(TAG, "handleOperation Enter");
        new Thread()
        {
            public void run()
            {
                WLog.d(TAG, "handleOperation Thread:" + op);
                switch (op)
                {
                    case OP_PULL_ML:
                    {
                        mCurrentFold = getPullMsgFold();
                        WLog.d(TAG, "handleOperation pull fold:" + mCurrentFold);
                        if (mCurrentFold < 0)
                        {
                            signalPullCmt();
                            gotoState(STATE_READY);

                        }
                        else
                        {
                            setMLSyncedStatus(STATE_SYNC_START);
                            mDevice.getMsgList(mCurrentFold, MAX_PULL_MSGS_IN_FOLD, 0);
                            removePullMsgFold(mCurrentFold);
                            waitPullCmt();
                        }
                        break;
                    }
                    case OP_PULL_ML_CONT:
                    {
                        WLog.d(TAG, "handleOperation CONTINUE");
                        mDevice.getMsgListCont();
                        waitPullCmt();
                        break;
                    }
                    case OP_PULL_ML_CMT:
                    {
                        mDevice.getMsgListCmt();
                        waitPullCmt(5);
                        break;
                    }
                    case OP_PULL_ML_CMT_EARLY:
                    {
                        mDevice.getMsgListCmt();
                        waitPullCmt(5);
                        break;
                    }
                    case OP_GET_MSG:
                    {
                        if (mRetriveMsgHandle != null)
                        {
                            mDevice.getMsg(mRetriveMsgHandle);
                            waitPullCmt();
                        }
                        else
                        {
                            gotoState(STATE_READY);
                        }
                        break;
                    }
                    case OP_SEND_MSG:
                    {
                        if (mSendMsgBuf.size() > 0)
                        {
                            SendMsg sendMsg = mSendMsgBuf.get(0);
                            mSendMsgBuf.remove(0);
                            if (sendMsg != null)
                            {
                                mDevice.sendMesssage(sendMsg.more, sendMsg.data);
                                waitPullCmt();
                            }
                            else
                            {
                                WLog.d(TAG, "sendMsg == null,goto ready");
                                gotoState(STATE_READY);
                            }
                        }
                        else
                        {
                            WLog.d(TAG, "mSendMsgBuf empty,goto ready");
                            gotoState(STATE_READY);
                        }
                        break;
                    }
                    case OP_DISCONNECT_SVC:
                    {
                        WLog.d(TAG, "Jon: [BT] disconnnect MAP SVC CP 2");
                        mDevice.mService.deviceConnect(mDevice.mAddress, BTConstants.BT_MAP, false);
                        initedState();
                    }
                    default:
                        break;
                }
            }
        }.start();
    }

    void handleGetMessageDataDone()
    {
        WLog.d(TAG, "handleListMoreDataIndication Enter");
        new Thread()
        {
            public void run()
            {
                WLog.d(TAG,
                        "Jon: [BT] handleListMoreDataIndication Enter,mOpFlag is:" + mOpFlag + ",needMore is:" + needMore);
                parserReceivedMessages();
                signalPullCmt();
                gotoState(STATE_READY);
            }
        }.start();
    }

    void handlePushMessageDataContinue()
    {
        WLog.d(TAG, "handlePushMessageDataContinue Enter");
        new Thread()
        {
            public void run()
            {
                WLog.d(TAG, "Jon: [BT] handlePushMessageDataContinue thread");
                if (mSendMsgBuf.size() > 0)
                {
                    mOpFlag = mOpFlag | OP_SEND_MSG;
                }
                signalPullCmt();
                gotoState(STATE_READY);
            }
        }.start();
    }


    void handlePushMessageDataComplete()
    {
        WLog.d(TAG, "handlePushMessageDataComplete Enter");
        new Thread()
        {
            public void run()
            {
                WLog.d(TAG, "Jon: [BT] handlePushMessageDataComplete thread");
                if (mSendMsgBuf.size() > 0)
                {
                    mOpFlag = mOpFlag | OP_SEND_MSG;
                }
                signalPullCmt();
                gotoState(STATE_READY);
            }
        }.start();
        onSendTransactionEnd(SENDMSG_REASON_SUCCESS);
    }


    void handleListMoreDataIndication()
    {
        WLog.d(TAG, "handleListMoreDataIndication Enter");
        new Thread()
        {
            public void run()
            {
                WLog.d(TAG,
                        "Jon: [BT] handleListMoreDataIndication Enter,mOpFlag is:" + mOpFlag + ",needMore is:" + needMore);
                if (needMore == 1)
                {
                    needMore = 0;
                    mOpFlag = mOpFlag | OP_PULL_ML_CONT;
                }
                else
                {
                    mOpFlag = mOpFlag | OP_PULL_ML_CMT;
                    parserReceivedMessagesList();
                }
                signalPullCmt();
                gotoState(STATE_READY);
            }
        }.start();
    }

    void handleMessageOperationComplete()
    {
        new Thread()
        {
            public void run()
            {
                WLog.d(TAG, "handleMessageOperationComplete Enter");
                int fold = getPullMsgFold();
                if (fold >= 0)
                {
                    mOpFlag = mOpFlag | OP_PULL_ML;
                }
                signalPullCmt();
                gotoState(STATE_READY);
            }
        }.start();
    }

    void parserReceivedMessages()
    {
        WLog.d(TAG, "parserReceivedMessages Enter");
        BluetoothMessage msg = null;
        if (mRetriveMsg != null)
        {
            msg = new BluetoothMessage();
            if (mRetriveMsgHandle != null)
            {
                if (mRetriveMsgHandle.length() >= 16)
                {
                    msg.accountId = Integer.valueOf(mRetriveMsgHandle.substring(0, 8), 16);
                    msg.msgId = Integer.valueOf(mRetriveMsgHandle.substring(8, 16), 16);
                }
                else
                {
                    if (mRetriveMsgHandle.length() < 8)
                    {
                        msg.accountId = Integer.valueOf(mRetriveMsgHandle);
                        msg.msgId = 0;
                    }
                    else
                    {
                        msg.accountId = Integer.valueOf(mRetriveMsgHandle.substring(0, 8), 16);
                        msg.msgId = Integer.valueOf(mRetriveMsgHandle.substring(8, mRetriveMsgHandle.length()), 16);
                    }
                }
            }
            String content = "";
            String[] ls = mRetriveMsg.split("\\n");
            int i = 0;
            do
            {
                WLog.d(TAG, "Jon: mRetriveMsg < " + ls[i]);
                String[] params = ls[i].split(":", 2);
                String name = params[0];
                String value = "";
                if (params.length > 1)
                {
                    value = params[1];
                }

                if ("TYPE".equals(name))
                {
                    if ("SMS_GSM".equals(value))
                    {
                        msg.type = MSG_GSM;
                    }
                    else if ("SMS_CDMA".equals(value))
                    {
                        msg.type = MSG_CDMA;
                    }
                }
                else if ("FN".equals(name))
                {
                    msg.senderName = value;
                }
                else if ("TEL".equals(name))
                {
                    msg.senderAddr = value;
                }
                else if ("BEGIN".equals(name))
                {
                    if ("MSG".equals(value))
                    {
                        while ((i + 1) < ls.length && !ls[i + 1].equals("END:MSG"))
                        {
                            i = i + 1;
                            WLog.d(TAG, "Jon: mRetriveMsg < " + ls[i]);
                            content = content + ls[i];
                        }
                    }
                }
                i = i + 1;
            } while (i < ls.length);
            msg.content = content;
        }

        if (msg != null)
        {
            if (msg.content == null)
            {
                msg.content = "";
            }
            WLog.d(TAG, "Retrived Message:" + msg);
            notifyMessagesRetriveEndChanged(msg.accountId, msg.msgId, msg.content);
        }
        mRetriveMsg = null;
        mRetriveMsgHandle = null;
    }

    void parserReceivedMessagesList()
    {
        WLog.d(TAG, "parserReceivedMessagesList Enter");

        if (mListPackets != null)
        {
            if (mMsgList == null)
            {
                mMsgList = new ArrayList<BluetoothMessage>();
            }
            BluetoothMessage msg = null;
            WLog.d(TAG, "mListPackets is:" + mListPackets.toString());
            try
            {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(new StringReader(mListPackets.toString()));
                int event = parser.getEventType();
                while (event != XmlPullParser.END_DOCUMENT)
                {
                    switch (event)
                    {
                        case XmlPullParser.START_DOCUMENT:
                            break;
                        case XmlPullParser.START_TAG:
                            if ("msg".equals(parser.getName()))
                            {
                                msg = new BluetoothMessage();
                                msg.fold = mCurrentFold;
                                int count = parser.getAttributeCount();
                                for (int i = 0; i < count; i++)
                                {
                                    // WLog.d(TAG,"key is:"+parser.getAttributeName(i));
                                    // WLog.d(TAG,"value is:"+parser.getAttributeValue(i));
                                    String key = parser.getAttributeName(i);
                                    if ("handle".equals(key))
                                    {
                                        String value = parser.getAttributeValue(i);
                                        msg.mHandle = value;
                                        if (value.length() < 16)
                                        {
                                            WLog.d(TAG, "maybe apple phone");
                                            if (value.length() < 8)
                                            {
                                                msg.accountId = Integer.valueOf(value);
                                                msg.msgId = 0;
                                            }
                                            else
                                            {
                                                msg.accountId = Integer.valueOf(value.substring(0, 8), 16);
                                                msg.msgId = Integer.valueOf(value.substring(8, value.length()), 16);
                                                return;
                                            }
                                        }
                                        else
                                        {
                                            msg.accountId = Integer.valueOf(value.substring(0, 8), 16);
                                            msg.msgId = Integer.valueOf(value.substring(8, 16), 16);
                                        }
                                        WLog.d(TAG, "msg.accountId is:" + msg.accountId + ",msg.msgId is:" + msg.msgId);
                                    }
                                    else if ("subject".equals(key))
                                    {
                                        msg.subject = parser.getAttributeValue(i);
                                        if (msg.subject == null || msg.subject.length() == 0)
                                        {
                                            msg.subject = "...";
                                        }
                                    }
                                    else if ("datetime".equals(key))
                                    {
                                        try
                                        {
                                            String value = parser.getAttributeValue(i);
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                                            java.util.Date d = sdf.parse(value.replaceAll("T", ""));
                                            msg.dateTime = d.getTime() / 1000; //to Unix timeStamp
                                        }
                                        catch (Exception ex)
                                        {
                                        }
                                    }
                                    else if ("sender_name".equals(key))
                                    {
                                        msg.senderName = parser.getAttributeValue(i);
                                    }
                                    else if ("sender_addressing".equals(key))
                                    {
                                        msg.senderAddr = parser.getAttributeValue(i);
                                    }
                                    else if ("recipient_name".equals(key))
                                    {
                                        msg.recipname = parser.getAttributeValue(i);
                                    }
                                    else if ("recipient_addressing".equals(key))
                                    {
                                        msg.recipaddr = parser.getAttributeValue(i);
                                    }
                                    else if ("type".equals(key))
                                    {
                                        String value = parser.getAttributeValue(i);
                                        if ("SMS_GSM".equals(value))
                                        {
                                            msg.type = MSG_GSM;
                                        }
                                        else if ("SMS_CDMA".equals(value))
                                        {
                                            msg.type = MSG_CDMA;
                                        }
                                    }
                                    else if ("size".equals(key))
                                    {
                                        msg.size = Integer.valueOf(parser.getAttributeValue(i));
                                    }
                                    else if ("text".equals(key))
                                    {
                                    }
                                    else if ("reception_status".equals(key))
                                    {
                                    }
                                    else if ("attachment_size".equals(key))
                                    {
                                    }
                                }
                            }
                            break;
                        case XmlPullParser.END_TAG:
                            if ("msg".equals(parser.getName()))
                            {
                                if (msg != null)
                                {
                                    if (msg.type == MSG_GSM || msg.type == MSG_CDMA)
                                    {
                                        if (mMsgList != null)
                                        {
                                            mMsgList.add(msg);
                                        }
                                    }
                                    msg = null;
                                }
                            }
                            break;
                    }
                    event = parser.next();
                }
            }
            catch (XmlPullParserException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        if (mMsgList != null)
        {
            for (int i = 0; i < mMsgList.size(); i++)
            {
                WLog.d(TAG, "JonSMS:" + mMsgList.get(i));
            }
            notifyMessagesListEndChanged();
            mListPackets = null;
        }
    }


    public List<BluetoothMessage> getMessages()
    {
        WLog.d(TAG, "getMessages Enter");
        return mMsgList;
    }


    void notifyMessagesListEndChanged()
    {
        Intent intent = new Intent("chleon.android.bluetooth.device.message.listend");
        mContext.sendBroadcast(intent);
    }


    void notifyMessagesRetriveEndChanged(int accountId, int msgId, String msgText)
    {
        //WLog.d(TAG,"notifyMessagesRetriveEndChanged, accountId is:"+accountId+
        // ",msgId:"+msgId+",msgText:"+msgText);
        Intent intent = new Intent("chleon.android.bluetooth.device.message.retrived");
        intent.putExtra("ACCOUNT", accountId);
        intent.putExtra("MSGID", msgId);
        intent.putExtra("CONTENT", msgText);
        mContext.sendBroadcast(intent);
    }

    /*
    SMS Deliver PDU
    00040d91683118804683f00000515060618323230131
    00  SCA
    04  PDUType
    0d91683118804683f0  OA
    00 PID
    00 DCS
    51506061832323  SCST
    01  UDL
    31  UD
    */
    boolean retriveMapMessage(int accountId, int msgId)
    {
        mRetriveMsgHandle = null;
        if (mMsgList != null)
        {
            WLog.d(TAG, "mMsgList != null");
            for (int i = 0; i < mMsgList.size(); i++)
            {
                ;
                if (accountId == mMsgList.get(i).accountId && msgId == mMsgList.get(i).msgId)
                {
                    mRetriveMsgHandle = mMsgList.get(i).mHandle;
                    break;
                }
            }
        }
        if (mRetriveMsgHandle != null)
        {
            WLog.d(TAG, "mRetriveMsgHandle  is:" + mRetriveMsgHandle);
            onEvtChange(EVT_GET_MSG);
            return true;
        }
        return false;
    }


    int getPullMsgFold()
    {
        if ((mPullFoldFlag & (1 << FOLD_INBOX)) > 0)
        {
            return FOLD_INBOX;
        }
        else if ((mPullFoldFlag & (1 << FOLD_SENT)) > 0)
        {
            return FOLD_SENT;
        }
        else
        {
            mPullFoldFlag = 0;
            return -1;
        }
    }

    void removePullMsgFold(int fold)
    {
        mPullFoldFlag = mPullFoldFlag & (~(1 << fold));
    }

    boolean disconnectService()
    {
        WLog.d(TAG, "disconnectService Enter");
        onEvtChange(EVT_DISCONNECT_SVC);
        return true;
    }

    boolean sendMapMessage(BluetoothMessage msg)
    {
        WLog.d(TAG, "sendMapMessage Enter");
        //mSendMsg = msg;
        String sendMsg = getComposeMessage(msg);
        WLog.d(TAG, "sendMsg str len is:" + sendMsg.length() + ",buf len is:" + sendMsg.getBytes().length);
        if (sendMsg == null)
        {
            WLog.d(TAG, "sendMapMessage null content,send false");
            return false;
        }
        //sendMsg="BEGIN:BMSG\r\nVERSION:1.0\r\nSTATUS:READ\r\nTYPE:SMS_GSM\r\nFOLDER:outbox\r\nBEGIN:VCARD\r\nVERSION:2.1\r\nN:\r\nEND:VCARD\r\nBEGIN:BENV\r\nBEGIN:VCARD\r\nVERSION:2.1\r\nN:\r\nTEL:13810864380\r\nEND:VCARD\r\nBEGIN:BBODY\r\nCHARSET:NATIVE\r\nENCODING:G-7BIT\r\nLENGTH:15\r\nBEGIN:MSG\r\nIt's a test!\r\nEND:MSG\r\nEND:BBODY\r\nEND:BENV\r\nEND:BMSG\r\n";
        {
            String[] ls = sendMsg.split("\\n");
            for (int i = 0; i < ls.length; i++)
            {
                WLog.d(TAG, "send out message > " + ls[i]);
            }
        }
        int len = 0;
        byte[] sendBuf = sendMsg.getBytes();
        while (len < sendBuf.length)
        {
            if ((len + MAX_SEND_MSG_LEN) < sendBuf.length)
            {
                SendMsg s = new SendMsg();
                byte[] data = new byte[MAX_SEND_MSG_LEN];
                System.arraycopy(sendBuf, len, data, 0, MAX_SEND_MSG_LEN);
                s.data = new String(data);
                s.more = 1;
                mSendMsgBuf.add(s);
            }
            else
            {
                SendMsg s = new SendMsg();
                int dataLen = sendBuf.length - len;
                byte[] data = new byte[dataLen];
                System.arraycopy(sendBuf, len, data, 0, dataLen);
                s.data = new String(data);
                s.more = 0;
                mSendMsgBuf.add(s);
            }
            len = len + MAX_SEND_MSG_LEN;
        }
        onEvtChange(EVT_SEND_MSG);
        return true;
    }

    void notifyMessagesEvtChanged(int evt)
    {
        Intent intent = new Intent("chleon.android.bluetooth.device.message.evt");
        intent.putExtra("EVT", evt);
        mContext.sendBroadcast(intent);
    }


    public String getComposeMessage(BluetoothMessage mMsg)
    {
        String ret = null;
        if (mMsg != null)
        {
            //tringBuffer mBuffer = new StringBuffer();
            String msgType = SMS_GSM;
            String msgEncode = "G-7BIT\r\n";
            if (mMsg.type == MSG_EMAIL)
            {
                msgType = EMAIL;
                return null;
            }
            else if (mMsg.type == MSG_CDMA)
            {
                msgType = SMS_CDMA;
                msgEncode = "C-UNICODE\r\n";
            }
            else if (mMsg.type == MSG_MMS)
            {
                msgType = MMS;
                return null;
            }
            else
            {
                msgType = SMS_GSM;
                msgEncode = "G-7BIT\r\n";
            }
            String msgTel = mMsg.recipaddr;
            String msgContent = mMsg.content;
            if (msgContent != null)
            {
                int msgLen = "BEGIN:MSG\r\n".length() + msgContent.getBytes().length + "\r\nEND:MSG\r\n".length();
                ret = "BEGIN:BMSG\r\nVERSION:1.0\r\nSTATUS:READ\r\nTYPE:" + msgType +
                        "FOLDER:outbox\r\nBEGIN:VCARD\r\nVERSION:2.1\r\nN:\r\nEND:VCARD\r\n" +
                        "BEGIN:BENV\r\nBEGIN:VCARD\r\nVERSION:2.1\r\nN:\r\nTEL:" + msgTel +
                        "\r\nEND:VCARD\r\nBEGIN:BBODY\r\nCHARSET:NATIVE\r\nENCODING:" + msgEncode +
                        "LENGTH:" + msgLen + "\r\nBEGIN:MSG\r\n" + msgContent + "\r\nEND:MSG\r\nEND:BBODY\r\nEND:BENV\r\nEND:BMSG\r\n";
            }
        }
        return ret;
    }
}

