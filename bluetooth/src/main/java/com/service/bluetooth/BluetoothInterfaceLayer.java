package com.service.bluetooth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

public class BluetoothInterfaceLayer extends BtBaseCommands implements BtCommandInterface
{
    static
    {
        System.loadLibrary("jni_bt");
    }

    native FileDescriptor nativeOpen() throws IOException;

    native void nativeClose(FileDescriptor fd) throws IOException;

    native int readMessage(byte[] buffer, int off, int len, FileDescriptor fd) throws IOException;

    native int nativeWrite(byte[] data, int offset, int length, FileDescriptor fd) throws IOException;

    native FileDescriptor nativeUpdate() throws IOException;


    static final String LOG_TAG = "ApuBTJ";
    static final boolean LOGD = true;
    static final boolean LOGV = true;
    static final boolean DEBUG_PAYLOAD = false;
    static final int OPEN_RETRY_MILLIS = 4 * 1000;

    static final int EVENT_SEND = 1;
    static final int EVENT_WAKE_LOCK_TIMEOUT = 2;

    // ***** Constants
    static final int MAX_COMMAND_FAILURE_COUNT = 5;
    static int sTimeoutCount = 0;

    static final int APU_MAX_COMMAND_BYTES = (1024);
    static final String RESPONSE_STATUS_ERROR = "ERROR";

    private static final byte EXTRA_DATA_LENGTH = 2;
    private static final int DEFAULT_WAKE_LOCK_TIMEOUT = 5000;
    static final int MAX_POOL_SIZE = 10;

    // ***** Instance Variables

    HandlerThread mSenderThread;
    APUSender mSender;
    Thread mReceiverThread;
    APUReceiver mReceiver;
    WakeLock mWakeLock;
    int mRequestMessagesPending;
    boolean mRequestSmsLoading;
    FileDescriptor mFileDescriptor;
    String mCommandCodes[];
    String mEventCodes[];
    final HashSet<String> mCommandIdSet, mEventIdSet;
    int mEventCode;
    ArrayBlockingQueue<BTRequest> mRequestsList = new ArrayBlockingQueue<BTRequest>(MAX_POOL_SIZE);

    Thread mNativeReceiverThread;
    APUNativeReceiver mNativeReceiver;
    private Object mBufLock = new Object();
    private Object mSenderLock = new Object();
    private Object mNativeFileLock = new Object();
    private boolean isReqOncompleted = true;

    private RingBuffer mRingBuffer = new RingBuffer();
    RequestQ mRequestQ; // normal
    RequestQ mRequestQ_L; // Lower Prior

    private Thread requestQSenderThread;
    private RequestQSender requestQSender;

    ArrayList<CallStatus> mCallStatusArray = null;
    ArrayList<BtPBContact> mPhoneBookArray = null;
    ArrayList<BluetoothMessage> mMapMessageArray = null;
    BluetoothMessage mRetrivedMessage = null;

    byte[] mTempBuf = new byte[APU_MAX_COMMAND_BYTES];

    Context mContext;

    boolean mIsUpdate = false;
    private final String PATH = "/dev/ttymxc1";
    private final int BAUDRATE = 115200;

    public BluetoothInterfaceLayer(Context context)
    {
        mContext = context;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG);
        mWakeLock.setReferenceCounted(false);
        mRequestMessagesPending = 0;

        mSenderThread = new HandlerThread("ApuSender");
        mSenderThread.start();

        Looper looper = mSenderThread.getLooper();
        mSender = new APUSender(looper);
        mCommandCodes = BTConstants.getCommands();
        mCommandIdSet = new HashSet<String>(mCommandCodes.length + 1);
        mCommandIdSet.add(RESPONSE_STATUS_ERROR);
        for (int i = 0; i < mCommandCodes.length; i++)
        {
            if (mCommandCodes[i] != null)
            {
                mCommandIdSet.add(mCommandCodes[i]);
            }
        }

        mEventCodes = BTConstants.getEventCommands();
        mEventIdSet = new HashSet<String>(mEventCodes.length);
        for (int i = 0; i < mEventCodes.length; i++)
        {
            if (mEventCodes[i] != null)
            {
                mEventIdSet.add(mEventCodes[i]);
            }
        }
        mRequestQ = new RequestQ(150);
        mRequestQ_L = new RequestQ(20);

        requestQSender = new RequestQSender();
        requestQSenderThread = new Thread(requestQSender, "RequestQ");
        requestQSenderThread.start();

        mReceiver = new APUReceiver();
        mReceiverThread = new Thread(mReceiver, "BTReceiver");
        mReceiverThread.start();

        mNativeReceiver = new APUNativeReceiver();
        mNativeReceiverThread = new Thread(mNativeReceiver, "BTNativeReceiver");
        mNativeReceiverThread.start();
        mIsUpdate = false;
    }

    /**
     * {@hide}
     */
    static class BTRequest implements Comparable<BTRequest>
    {

        // ***** Class Variables
        static byte sNextSerial = 0;
        static Object sSerialMonitor = new Object();
        private static Object sPoolSync = new Object();
        private static BTRequest sPool = null;
        private static int sPoolSize = 0;

        // ***** Instance Variables
        byte mSerial;
        int mRequest;
        String mCommand;
        Message mResult;
        ByteBuffer mBuffer;
        Object mData;
        BTRequest mNext;

        private boolean mIsSyncSend = true;

        boolean isSendSync()
        {
            return mIsSyncSend;
        }

        void setSendSync(boolean on)
        {
            mIsSyncSend = on;
        }

        /**
         * Retrieves a new APURequest instance from the pool.
         *
         * @param request GID from starts with GID_*
         * @param result  sub group ID from
         * @param result  sent when operation completes
         * @return a APURequest instance from the pool.
         */
        static BTRequest obtain(int request, Message result)
        {
            BTRequest rr = null;

            synchronized (sPoolSync)
            {
                if (sPool != null)
                {
                    rr = sPool;
                    sPool = rr.mNext;
                    rr.mNext = null;
                    sPoolSize--;
                }
            }

            if (rr == null)
            {
                rr = new BTRequest();
            }

            synchronized (sSerialMonitor)
            {
                rr.mSerial = sNextSerial++;
            }
            rr.mRequest = request;
            rr.mResult = result;
            rr.mBuffer = ByteBuffer.allocate(APU_MAX_COMMAND_BYTES);
            rr.mIsSyncSend = true;

            if (result != null && result.getTarget() == null)
            {
                throw new NullPointerException("Message target must not be null");
            }

            return rr;
        }

        /**
         * Returns a APURequest instance to the pool.
         * <p/>
         * Note: This should only be called once per use.
         */
        void release()
        {
            synchronized (sPoolSync)
            {
                if (sPoolSize < MAX_POOL_SIZE)
                {
                    this.mNext = sPool;
                    sPool = this;
                    sPoolSize++;
                }
            }
        }

        private BTRequest()
        {
        }

        static void resetSerial()
        {
            synchronized (sSerialMonitor)
            {
                sNextSerial = 0;
            }
        }

        String serialString()
        {
            StringBuilder sb = new StringBuilder(8);
            sb.append('[');
            sb.append(Byte.toString(mSerial));
            sb.append(']');
            return sb.toString();
        }

        void onError(int error, Object ret)
        {
            CommandException ex;

            ex = CommandException.fromBTErrno(error);

            if (BluetoothInterfaceLayer.LOGD)
            {
                WLog.d(LOG_TAG, serialString() + "< " + BTConstants.requestToString(mRequest) + " error: " + ex);
            }

            if (mResult != null && mResult.getTarget() != null)
            {
                AsyncResult.forMessage(mResult, ret, ex);
                mResult.sendToTarget();
            }

            if (mBuffer != null)
            {
                mBuffer = null;
            }
        }

        @Override
        public int compareTo(BTRequest another)
        {
            return mSerial - another.mSerial;
        }
    }

    private void dumpbuffer(ByteBuffer p)
    {
        WLog.d(LOG_TAG, "Jon [BT]: +++++++++++++DUMP++++++++++++++");
        p.mark();
        while (p.remaining() > 0)
        {
            byte b = p.get();
        }
        WLog.d(LOG_TAG, "Jon [BT]: -------------DUMP--------------");
        p.reset();

    }

    class APUReceiver implements Runnable
    {
        APUReceiver()
        {
        }

        public void run()
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
            try
            {
                for (; ; )
                {
                    int length = 0;
                    try
                    {
                        for (; ; )
                        {
                            ByteBuffer byteBuffer;
                            // WLog.d(LOG_TAG,"Jon: [BT] APUReceiver readLine");
                            byteBuffer = readLine();

                            if (byteBuffer == null)
                            {
                                // End-of-stream reached
                                continue;
                            }

                            String command = getCommand(byteBuffer);
                            WLog.v("command = " + command);
                            boolean solicited = isSolicited(command);
                            WLog.v("solicited = " + solicited);
                            if (solicited)
                            {
                                processSolicited(command, byteBuffer);
                                isReqOncompleted = true;
                            }
                            else
                            {
                                processUnsolicited(command, byteBuffer);
                            }

                            releaseWakeLockIfDone();
                            byteBuffer = null;
                        }
                    }
                    catch (Throwable tr)
                    {
                        WLog.e(LOG_TAG,
                                "Jon: [BT] Uncaught exception read length=" + length + "Exception:" + tr.getMessage(),
                                tr);
                    }

                    WLog.i(LOG_TAG, "Jon: [BT] Disconnected from BT Device");
                    BTRequest.resetSerial();

                    // Clear request list on close
                    synchronized (mRequestsList)
                    {
                        Iterator<BTRequest> iterator = mRequestsList.iterator();
                        while (iterator.hasNext())
                        {
                            BTRequest rr = (BTRequest) iterator.next();
                            rr.onError(BTConstants.BT_NOT_AVAILABLE, null);
                            rr.release();
                        }

                        mRequestsList.clear();
                    }
                }
            }
            catch (Throwable tr)
            {
                WLog.e(LOG_TAG, "Jon: [BT] Uncaught exception", tr);
            }
        }
    }

    class RequestQSender implements Runnable
    {
        private int COMMAND_HANLER_TIME_OUT = 50;
        private int timeout = 0;

        RequestQSender()
        {
            isReqOncompleted = true;
        }

        public void run()
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
            for (; ; )
            {
                do
                {
                    if (!mRequestQ.isEmpty() || !mRequestQ_L.isEmpty())
                    {
                        if (isReqOncompleted || timeout <= 0)
                        {
                            timeout = COMMAND_HANLER_TIME_OUT;
                            isReqOncompleted = false;
                            BTRequest rr = null;
                            if (!mRequestQ.isEmpty())
                            {
                                rr = mRequestQ.get();
                            }
                            else
                            {
                                rr = mRequestQ_L.get();
                            }
                            WLog.v("rr = " + rr);
                            if (rr != null)
                            {
                                Message msg = mSender.obtainMessage(EVENT_SEND, rr);
                                acquireWakeLock();
                                msg.sendToTarget();
                            }
                        }
                        else
                        {
                            if (timeout > 0)
                            {
                                try
                                {
                                    Thread.sleep(100);
                                }
                                catch (InterruptedException er)
                                {
                                }
                                timeout--;
                            }
                            else
                            {
                                // timeOut
                                WLog.d(LOG_TAG, "Jon: RequestQSender timeOut");
                                isReqOncompleted = true;
                            }
                        }
                    }
                    else
                    {
                        synchronized (mSenderLock)
                        {
                            try
                            {
                                mSenderLock.wait();
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                } while (true);
            }
        }
    }

    class RequestQ
    {
        private static final int DEFAULT_SIZE = 50;
        private BTRequest[] mBuf;
        private int maxSize = 0;
        public int iRead = 0;
        public int iWrite = 0;
        private boolean full = false;
        private Object mLock = new Object();

        public RequestQ()
        {
            this(DEFAULT_SIZE);
        }

        public RequestQ(int capacity)
        {
            if (capacity <= 0)
            {
                throw new IllegalArgumentException("Illegal Capacity: " + capacity);
            }
            mBuf = new BTRequest[capacity];
            maxSize = capacity;
            iRead = 0;
            iWrite = 0;
        }

        public String toString()
        {
            return "maxSize is:" + maxSize + ",iRead is:" + iRead + ",iWrite is:" + iWrite + ",full is:" + full + ",remain is:" + remain();
        }

        private void reset()
        {
            synchronized (mLock)
            {
                iRead = 0;
                iWrite = 0;
                full = false;
            }
        }

        public int size()
        {
            int size = 0;
            if (iWrite < iRead)
            {
                size = maxSize - iRead + iWrite;
            }
            else if (iWrite == iRead)
            {
                size = (full ? maxSize : 0);
            }
            else
            {
                size = iWrite - iRead;
            }
            return size;
        }

        public int remain()
        {
            return maxSize - size();

        }

        public boolean isEmpty()
        {

            return size() == 0;
        }

        public boolean put(BTRequest item)
        {
            if (isFull())
            {
                throw new IllegalArgumentException("Jon: put to full buffer !!!!!!!!!!");
            }
            synchronized (mLock)
            {
                mBuf[iWrite++] = item;

                if (iWrite >= maxSize)
                {
                    iWrite = 0;
                }
                if (iWrite == iRead)
                {
                    full = true;
                }
            }
            return true;
        }

        public boolean isFull()
        {
            return size() == maxSize;
        }

        public BTRequest get()
        {
            if (isEmpty())
            {
                throw new IllegalArgumentException("Jon: get from empty buffer !!!!!!!!!!");
            }
            BTRequest element = null;
            synchronized (mLock)
            {
                element = mBuf[iRead++];
                if (iRead >= maxSize)
                {
                    iRead = 0;
                }
                full = false;
            }
            return element;
        }
    }

    class RingBuffer
    {
        private static final int DEFAULT_BUFFER_SIZE = 500 * 1024;
        private byte[] mBuf;
        private int maxSize = 0;
        public int iRead = 0;
        public int iWrite = 0;
        private boolean full = false;
        byte[] inBuf = null;
        private Object mLock = new Object();

        public RingBuffer()
        {
            this(DEFAULT_BUFFER_SIZE);
            inBuf = new byte[APU_MAX_COMMAND_BYTES];
            maxSize = DEFAULT_BUFFER_SIZE;
        }

        public RingBuffer(int capacity)
        {
            if (capacity <= 0)
            {
                throw new IllegalArgumentException("Illegal Capacity: " + capacity);
            }
            mBuf = new byte[capacity];
            inBuf = new byte[APU_MAX_COMMAND_BYTES];
            maxSize = capacity;
            iRead = 0;
            iWrite = 0;
        }

        public String toString()
        {
            return "maxSize is:" + maxSize + ",iRead is:" + iRead + ",iWrite is:" + iWrite + ",full is:" + full + ",remain is:" + remain();
        }

        private void reset()
        {
            synchronized (mLock)
            {
                iRead = 0;
                iWrite = 0;
                full = false;
            }
        }

        public int size()
        {
            int size = 0;
            if (iWrite < iRead)
            {
                size = maxSize - iRead + iWrite;
            }
            else if (iWrite == iRead)
            {
                size = (full ? maxSize : 0);
            }
            else
            {
                size = iWrite - iRead;
            }
            return size;
        }

        public int remain()
        {
            return maxSize - size();

        }

        public boolean isEmpty()
        {

            return size() == 0;
        }

        public boolean put(byte item)
        {
            if (isFull())
            {
                throw new IllegalArgumentException("Jon: put to full buffer !!!!!!!!!!");
            }
            synchronized (mLock)
            {
                mBuf[iWrite++] = item;
            }

            if (iWrite >= maxSize)
            {
                iWrite = 0;
            }
            if (iWrite == iRead)
            {
                full = true;
            }
            return true;
        }

        public boolean isFull()
        {
            return size() == maxSize;
        }

        public byte get()
        {
            byte element;
            if (isEmpty())
            {
                throw new IllegalArgumentException("Jon: get from empty buffer !!!!!!!!!!");
            }

            synchronized (mLock)
            {
                element = mBuf[iRead++];
            }

            if (iRead >= maxSize)
            {
                iRead = 0;
            }
            full = false;
            return element;
        }

        public boolean findNextEOL()
        {
            // WLog.d(LOG_TAG,"Jon: findNextEOL Enter");
            int cur = iRead;
            while ((cur != iWrite) && !isEOFLine(mBuf[cur]))
            {
                cur++;
                if (cur >= maxSize)
                {
                    cur = 0;
                }
            }
            return ((cur != iWrite) && isEOFLine(mBuf[cur])) ? true : false;
        }

        private void dump()
        {
            WLog.d(LOG_TAG, "Jon [BT]: +++++++++++++DUMP++++++++++++++");
            int cur = iRead;
            while (cur != iWrite)
            {
                byte b = mBuf[cur++];
                if (cur >= maxSize)
                {
                    cur = 0;
                }
            }
            WLog.d(LOG_TAG, "Jon [BT]: -------------DUMP--------------");
        }

        private boolean isEOFLine(byte b)
        {
            return (b == '\r');
        }

        public String readLine()
        {
            String line = "";
            int cur = iRead;
            // remove '\r\n' at first
            while ((cur != iWrite) && (isEOFLine(mBuf[cur])))
            {
                cur++;
                if (cur >= maxSize)
                {
                    cur = 0;
                }
            }
            iRead = cur;
            if (cur == iWrite) // empty
            {
                return line;
            }
            if (findNextEOL())
            {
                int index = 0;
                byte b;
                while (!isEOFLine(mBuf[iRead]))
                {
                    inBuf[index++] = get();
                }
                inBuf[index] = '\0';
                line = new String(inBuf, 0, index);

                // try remove '\r' '\n' end
                cur = iRead;
                while ((cur != iWrite) && isEOFLine(mBuf[cur]))
                {
                    cur++;
                    if (cur >= maxSize)
                    {
                        cur = 0;
                    }
                }
                if (cur != iWrite)
                {
                    iRead = cur;
                }
            }
            return line;
        }
    }

    private class APUNativeReceiver implements Runnable
    {
        byte[] buffer;

        APUNativeReceiver()
        {
            buffer = new byte[APU_MAX_COMMAND_BYTES];
        }

        public void run()
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
            int retryCount = 0;
            WLog.d(LOG_TAG, "APUNativeReceiver Enter");
            try
            {
                for (; ; )
                {
                    try
                    {
                        mFileDescriptor = nativeOpen();
                        if (mFileDescriptor == null)
                        {
                            throw new IOException("nativeOpen failed.");
                        }
                        else
                        {
                            synchronized (mNativeFileLock)
                            {
                                mNativeFileLock.notifyAll();
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        // don't print an error message after the the first time
                        // // or after the 8th time
                        if (retryCount == 8)
                        {
                            WLog.e(LOG_TAG,
                                    "Jon: Couldn't open device after " + retryCount + " times, continuing to retry silently");
                        }
                        else if (retryCount > 0 && retryCount < 8)
                        {
                            WLog.i(LOG_TAG, "Couldn't open device; retrying after timeout");
                        }

                        try
                        {
                            Thread.sleep(OPEN_RETRY_MILLIS);
                        }
                        catch (InterruptedException er)
                        {
                        }
                        retryCount++;
                        continue;
                    }
                    WLog.i(LOG_TAG, "JNI open success.");
                    for (; ; )
                    {
                        int countRead = 0;
                        if (mFileDescriptor == null)
                        {
                            synchronized (mNativeFileLock)
                            {
                                try
                                {
                                    mNativeFileLock.wait();
                                }
                                catch (InterruptedException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        }
                        countRead = readMessage(buffer, 0, APU_MAX_COMMAND_BYTES, mFileDescriptor);

                        if (countRead < 0)
                        {
                            WLog.e(LOG_TAG, "Hit EOS reading message length");
                        }
                        else
                        {
                            if (countRead > mRingBuffer.remain())
                            {
                                WLog.d(LOG_TAG, "[BT] read Message exceed the size of buffer remain:" + mRingBuffer
                                        .remain() + "countRead:" + countRead);
                                synchronized (mBufLock)
                                {
                                    if (mRingBuffer.findNextEOL())
                                    {
                                        mBufLock.notifyAll();
                                    }
                                    else
                                    {
                                        mRingBuffer.reset();
                                    }
                                }
                            }
                            else
                            {
                                if (countRead > 0)
                                {
                                    synchronized (mBufLock)
                                    {
                                        for (int i = 0; i < countRead; i++)
                                            mRingBuffer.put(buffer[i]);
                                        if (mRingBuffer.findNextEOL())
                                        {
                                            mBufLock.notifyAll();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                WLog.w(LOG_TAG, "IOException", ex);
            }
            catch (Throwable tr)
            {
                WLog.e(LOG_TAG, "Uncaught exception read,Exception:" + tr.getMessage());
            }
        }
    }

    ByteBuffer readLine()
    {
        //WLog.d(LOG_TAG,"Jon: [BT] readLine Enter ++++");
        ByteBuffer ret = null;
        do
        {
            synchronized (mBufLock)
            {
                if (mRingBuffer.findNextEOL())
                {
                    String line = mRingBuffer.readLine();
                    if (line.length() > 0)
                    {
                        ret = ByteBuffer.wrap(line.getBytes());
                        //WLog.d(LOG_TAG, "Jon: [BT] < " + line);
                        String[] ls = line.split("\\n");
                        for (int i = 0; i < ls.length; i++)
                        {
                            WLog.d(LOG_TAG, "Jon: [BT] < " + ls[i]);
                        }
                    }
                }
                else
                {
                    try
                    {
                        mBufLock.wait();
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        } while (ret == null);
        //dumpbuffer(ret);
        return ret;
    }

    /**
     * Holds a PARTIAL_WAKE_LOCK whenever a) There is outstanding APU request
     * sent to BT daemon and no replied b) There is a request waiting to be sent
     * out.
     * <p/>
     * There is a WAKE_LOCK_TIMEOUT to release the lock, though it shouldn't
     * happen often.
     */

    private void acquireWakeLock()
    {
        synchronized (mWakeLock)
        {
            mWakeLock.acquire();
            mRequestMessagesPending++;

            mSender.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
            Message msg = mSender.obtainMessage(EVENT_WAKE_LOCK_TIMEOUT);
            mSender.sendMessageDelayed(msg, DEFAULT_WAKE_LOCK_TIMEOUT);
        }
    }

    private void releaseWakeLockIfDone()
    {
        synchronized (mWakeLock)
        {
            if (mWakeLock.isHeld() && (mRequestMessagesPending == 0) && (mRequestsList.isEmpty()))
            {
                mSender.removeMessages(EVENT_WAKE_LOCK_TIMEOUT);
                mWakeLock.release();
            }
        }
    }

    private void send(BTRequest rr)
    {
        rr.mCommand = mCommandCodes[rr.mRequest];
        mRequestQ.put(rr);
        synchronized (mSenderLock)
        {
            mSenderLock.notifyAll();
        }
    }

    private void send_L(BTRequest rr)
    {
        rr.mCommand = mCommandCodes[rr.mRequest];
        mRequestQ_L.put(rr);
        synchronized (mSenderLock)
        {
            mSenderLock.notifyAll();
        }
    }

    private void sendAsyc(BTRequest rr)
    {
        rr.mCommand = mCommandCodes[rr.mRequest];
        rr.setSendSync(false);
        mRequestQ.put(rr);
        synchronized (mSenderLock)
        {
            mSenderLock.notifyAll();
        }
    }

    BTRequest removeFirstRequestFromList()
    {
        synchronized (mRequestsList)
        {
            return mRequestsList.poll();
        }
    }

    BTRequest getFirstRequestFromList()
    {
        synchronized (mRequestsList)
        {
            return mRequestsList.peek();
        }
    }

	/*
     * BTRequest findAndRemoveRequestFromList(String command) { synchronized
	 * (mRequestsList) { // TODO map command with send command // for now return
	 * request which goes first // since ideally, we should not send command //
	 * until response is received for first. return mRequestsList.poll(); } }
	 */

    boolean isSolicited(String response)
    {
        if (mRequestsList.size() > 0 && mCommandIdSet.contains(response))
        {
            BTRequest rr = getFirstRequestFromList();
            if (isMatchedResponse(rr, response))
            {
                return true;
            }
        }
        return false;
    }

    void getEventCodeFrom(String command)
    {
        //        int index = 0;
        //        mEventCode = 0;
        //        for (String event : mEventCodes)
        //        {
        //            index++;
        //            if (event.equals(command))
        //            {
        //                mEventCode = index;
        //            }
        //        }
        mEventCode = BTConstants.getEventCode(command);
    }

    void processTest(ByteBuffer byteBuffer)
    {
        synchronized (mRequestsList)
        {
            Iterator<BTRequest> iterator = mRequestsList.iterator();
            while (iterator.hasNext())
            {
                BTRequest rr = (BTRequest) iterator.next();
                rr.onError(BTConstants.BT_NOT_AVAILABLE, null);
                rr.release();
            }
            mRequestsList.clear();
        }
    }

    void processSolicited(String command, ByteBuffer byteBuffer)
    {
        BTRequest rr = removeFirstRequestFromList();

        if (rr == null)
        {
            WLog.w(LOG_TAG,
                    "Jon: [BT] Unexpected solicited response!!!!!! " + " cmd: " + command + ",try enent handler");
            // try handle it as Unsolid message
            if (mEventIdSet.contains(command))
            {
                processUnsolicited(command, byteBuffer);
                return;
            }
            WLog.d(LOG_TAG, "Jon: Unrecognized solicited response: " + command);
            return;
        }

        if (LOGV)
        {
            WLog.v(LOG_TAG, "JonXX:  processSolicited command = " + rr.mCommand);
        }
        if (LOGV)
        {
            WLog.v(LOG_TAG, "JonXX:  processSolicited response = " + command);
        }

        if (command.startsWith(RESPONSE_STATUS_ERROR))
        {
            rr.release();
            return;
        }

        Object ret = null;
        byteBuffer.get(); // remove space
        try
        {
            switch (rr.mRequest)
            {
                case BTConstants.BT_REQUEST_SEND_CMD:
                {
                    ret = handleReqSendCmd(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_GVER:
                {
                    ret = handleReqGVER(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_GLBD:
                {
                    ret = handleReqGLBD(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_GLDN:
                {
                    ret = handleReqGLDN(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_SLDN:
                {
                    ret = handleReqSLDN(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_GRDN:
                {
                    ret = handleReqGRDN(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_SPIN:
                {
                    ret = handleReqSPIN(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_GPIN:
                {
                    ret = handleReqGPIN(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_GPRD:
                {
                    ret = handleReqGPRD(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_DPRD:
                {
                    ret = handleReqDPRD(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_INQU:
                {
                    ret = handleReqINQU(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_PAIR:
                {
                    ret = handleReqPAIR(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_SCAN:
                {
                    ret = handleReqSCAN(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_EDFU:
                {
                    ret = handleReqEDFU(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_UART:
                {
                    ret = handleReqUART(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_SCOD:
                {
                    ret = handleReqSCOD(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_GCOD:
                {
                    ret = handleReqGCOD(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_SPRO:
                {
                    ret = handleReqSPRO(rr, command, byteBuffer);
                    break;
                }
                // HFP
                case BTConstants.BT_REQUEST_HFCONN:
                {
                    ret = handleReqHFCONN(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFDISC:
                {
                    ret = handleReqHFDISC(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFANSW:
                {
                    ret = handleReqHFANSW(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFCHUP:
                {
                    ret = handleReqHFCHUP(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFDIAL:
                {
                    ret = handleReqHFDIAL(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFDTMF:
                {
                    ret = handleReqHFDTMF(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFCTRS:
                {
                    ret = handleReqHFCTRS(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFMCAL:
                {
                    ret = handleReqHFMCAL(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFCLCC:
                {
                    ret = handleReqHFCLCC(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFSVGS:
                {
                    ret = handleReqHFSVGS(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFGVGS:
                {
                    ret = handleReqHFGVGS(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFSVGM:
                {
                    ret = handleReqHFSVGM(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFGVGM:
                {
                    ret = handleReqHFGVGM(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFMUTE:
                {
                    ret = handleReqHFMUTE(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFSCFG:
                {
                    ret = handleReqHFSCFG(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_HFGCFG:
                {
                    ret = handleReqHFGCFG(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_A2DPCONN:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_A2DPDISC:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_A2DPSVGS:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_A2DPGVGS:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_AVRCPPLAY:
                {
                    ret = handleReqAVRCPPLAY(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_AVRCPPAUSE:
                {
                    ret = handleReqAVRCPPAUSE(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_AVRCPSTOP:
                {
                    ret = handleReqAVRCPSTOP(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_AVRCPFORWARD:
                {
                    ret = handleReqAVRCPFORWARD(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_AVRCPBACKWARD:
                {
                    ret = handleReqAVRCPBACKWARD(rr, command, byteBuffer);
                    break;
                }
                case BTConstants.BT_REQUEST_AVRCPVOLUMEUP:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_AVRCPVOLUMEDOWN:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_AVRCPSABSVOL:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_PBCCONN:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_PBCDISC:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_PBCPULLPB:
                {//async
                    break;
                }
                case BTConstants.BT_REQUEST_PBCPULLCONT:
                {//async
                    break;
                }
                case BTConstants.BT_REQUEST_PBCPULLCRT:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_PBCPULLCMT:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_PBCSETPARSE:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_PBCGETPARSE:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_HIDCONN:
                {
                    break;
                }
                case BTConstants.BT_REQUEST_HIDDISC:
                {
                    break;
                }
                default:
                {
                    if (mEventIdSet.contains(command))
                    {
                        processUnsolicited(command, byteBuffer);
                        return;
                    }
                    WLog.d(LOG_TAG, "Jon: Unrecognized solicited response: " + rr.mRequest);
                    return;
                }
            }
        }
        catch (Throwable tr)
        {
            // Exceptions here usually mean invalid BT responses

            WLog.w(LOG_TAG, rr.serialString() + "< " + BTConstants
                    .requestToString(rr.mRequest) + " exception, possible invalid BT response", tr);

            if (rr.mResult != null)
            {
                AsyncResult.forMessage(rr.mResult, null, tr);
                rr.mResult.sendToTarget();
            }
            rr.release();
            return;
        }
        if (LOGD)
        {
            apujLog(rr.serialString() + "< " + BTConstants.requestToString(rr.mRequest) + " " + retToString(ret));
        }

        if (rr.mResult != null)
        {
            AsyncResult.forMessage(rr.mResult, ret, null);
            rr.mResult.sendToTarget();
        }

        rr.release();

    }

    void processUnsolicited(String command, ByteBuffer byteBuffer)
    {
        getEventCodeFrom(command);
        unsljLog(mEventCode);
        WLog.d("mEventCode = " + mEventCode);
        Object ret = null;
        if (byteBuffer.remaining() > 0)
        {
            byteBuffer.get(); // remove space
        }
        try
        {
            switch (mEventCode)
            {
                case BTConstants.BT_UNSOL_RESPONSE_INIT:
                {
                    handlerUnsolidRespINIT(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_ROLE:
                {
                    handlerUnsolidRespROLE(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_INQR:
                    handlerUnsolidRespINQR(command, byteBuffer);
                    break;
                case BTConstants.BT_UNSOL_RESPONSE_INQC:
                    handlerUnsolidRespINQC(command, byteBuffer);
                    break;
                case BTConstants.BT_UNSOL_RESPONSE_PAIR:
                {
                    handlerUnsolidRespPAIR(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFSTAT:
                {
                    handlerUnsolidRespHFSTAT(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFCONN:
                {
                    handlerUnsolidRespHFCONN(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFDISC:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFRING:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFIBRN:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFAUDIO:
                {
                    handlerUnsolidRespHFAUDIO(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFCLIP:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFCCWA:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFNUML:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFNUMC:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFSGNL:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFROAM:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFBATC:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFVGSI:
                {
                    handlerUnsolidRespHFVGSI(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFVGMI:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFSRVC:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFCHLD:
                {

                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_HFCCIN:
                {
                    handlerUnsolidRespHFCCIN(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_A2DPSTAT:
                {
                    handlerUnsolidRespA2DPSTAT(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_A2DPCONN:
                {
                    handlerUnsolidRespA2DPCONN(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_PBCSTAT:
                {
                    handlerUnsolidRespPBCSTAT(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_PBCCONN:
                {
                    handlerUnsolidRespPBCCONN(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_A2DPAUDIO:
                {
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_AVRCPSTAT:
                {
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_AVRCPCONN:
                {
                    handlerUnsolidRespAVRCPCONN(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_AVRCPDISC:
                {
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_AVRCPTITLE:
                {
                    handlerUnsolidRespAVRCPTITLE(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_AVRCPARTIST:
                {
                    handlerUnsolidRespAVRCPARTIST(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_PLAYSTATUS:
                {
                    handlerUnsolidRespPLAYSTATUS(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_AVRCPFEATURE:
                {
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_PBPULLDATAIND:
                {
                    handlerUnsolidRespPBPULLDATAIND(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_PBCPULLCMTIND:
                {
                    handlerUnsolidRespPBCPULLCMTIND(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_PBCPARSEDATAIND:
                {
                    handlerUnsolidRespPBCPARSEDATAIND(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_PBCPULLPB:
                {
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_PBCPULLCONT:
                {
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_PBCPULLCRT:
                {
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_PBCPULLCMT:
                {
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_GRDN:
                {
                    handlerUnsolidRespGRDN(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_MAPCINIT:
                {
                    handlerUnsolidRespMAPCINIT(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_MAPCCONN:
                {
                    handlerUnsolidRespMAPCCONN(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_MAPCDISC:
                {
                    handlerUnsolidRespMAPCDISC(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_MAPCGETDATAIND:
                {
                    handlerUnsolidRespMAPCGETDATAIND(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_MAPCGETCMTIND:
                {
                    handlerUnsolidRespMAPCGETCMTIND(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_MAPCPUSHCONTIND:
                {
                    handlerUnsolidRespMAPCPUSHCONTIND(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_MAPCPUSHCMTIND:
                {
                    handlerUnsolidRespMAPCPUSHCMTIND(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_MAPCEVTIND:
                {
                    handlerUnsolidRespMAPCEVTIND(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_GLDN:
                {
                    handlerUnsolidRespGLDN(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_MUTE:
                {
                    handlerUnsolidRespMUTE(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_GPRL:
                {
                    handlerUnsolidRespGPRL(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_GLBD:
                {
                    handlerUnsolidRespGLBD(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_RING:
                {
                    handlerUnsolidRespRING(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_AVRCPPLAY:
                {
                    handlerUnsolidRespAVRCPPLAY(command, byteBuffer);
                    break;
                }
                case BTConstants.BT_UNSOL_RESPONSE_AVRCPPAUSE:
                {
                    handlerUnsolidRespAVRCPPAUSE(command, byteBuffer);
                    break;
                }

                default:
                    break;
            }
        }
        catch (Throwable tr)
        {
            WLog.e(LOG_TAG, "Exception processing unsol response: " + mEventCode + "Exception:" + tr.toString());
            return;
        }
    }

    Object responseByte(ByteBuffer p)
    {
        byte response;

        response = p.get();

        return response;
    }


    Object responseInt(ByteBuffer p, int radix)
    {
        return Integer.parseInt(responseWord(p), radix);
    }

    int responseUnsignedInt(ByteBuffer p)
    {
        char c;
        int i = 0;
        while (p.remaining() > 0)
        {
            p.mark();
            c = (char) p.get();
            if (!Character.isDigit(c))
            {
                break;
            }
            i = i * 10 + (c - '0');
        }
        if (p.remaining() > 0)
        {
            p.reset();
        }
        return i;
    }

    String responsePrintableString(ByteBuffer p)
    {
        final int length = p.remaining();
        StringBuilder builder = new StringBuilder(length);
        byte b;
        for (int i = 0; i < length; i++)
        {
            b = p.get();
            if (/* Character.isWhitespace(b) */b == 0)
            {
                continue;
            }
            builder.append((char) b);
        }

        return builder.toString();
    }

    String getPrintableString(ByteBuffer p)
    {
        final int length = p.remaining();
        StringBuilder builder = new StringBuilder(length);
        byte b;
        p.mark();
        for (int i = 0; i < length; i++)
        {
            b = p.get();
            if (/* Character.isWhitespace(b) */b == 0)
            {
                continue;
            }
            builder.append((char) b);
        }
        p.reset();
        return builder.toString();
    }

    String getCommand(ByteBuffer p)
    {
        if (p == null || p.remaining() == 0)
        {
            return null;
        }
        String header = responseWord(p, (byte) 0x20);
        if ("AT+B".equals(header) || "AT-B".equals(header))
        {
            p.get();// remove space
            return responseWord(p, (byte) 0x20);
        }
        return null;
    }

    String responseWord(ByteBuffer p, final byte split)
    {
        if (p == null || p.remaining() == 0)
        {
            return null;
        }
        int index = 0;
        byte b = 0;
        boolean doReset = false;
        outer:
        while (p.remaining() > 0)
        {
            p.mark();
            b = p.get();
            if (b == split || b == 0x0D || b == 0x0A)
            {
                doReset = true;
                break outer;
            }
            mTempBuf[index++] = b;
        }
        if (doReset)
        {
            p.reset();
        }
        return new String(mTempBuf, 0, index);
    }

    String responseWord(ByteBuffer p)
    {
        if (p == null || p.remaining() == 0)
        {
            return null;
        }
        int index = 0;
        byte b = 0;
        // byte[] mTempBuf = new byte[APU_MAX_COMMAND_BYTES];
        boolean doReset = false;
        outer:
        while (p.remaining() > 0)
        {
            p.mark();
            b = p.get();
            switch (b)
            {
                case 0x0D:
                case 0x0A:
                case ',':
                    doReset = true;
                    break outer;
            }
            mTempBuf[index++] = b;
        }
        if (doReset)
        {
            p.reset();
        }
        return new String(mTempBuf, 0, index);
    }

    Object responseString(ByteBuffer p)
    {
        p.get(); // skip "'"
        String data = responseWord(p);
        p.get(); // skip "'"
        return data;
    }

    Object responseFullString(ByteBuffer p)
    {
        p.get(); // skip "'"
        StringBuilder builder = new StringBuilder();
        while (p.remaining() > 1)
        {
            builder.append((char) p.get());
        }
        p.get(); // skip "'"
        return builder.toString();
    }


    String responseAllAsWord(ByteBuffer p)
    {
        if (p == null || p.remaining() == 0)
        {
            return null;
        }
        int index = 0;
        byte b = 0;
        boolean doReset = false;
        outer:
        while (p.remaining() > 0)
        {
            p.mark();
            b = p.get();
            switch (b)
            {
                case 0x0D:
                    doReset = true;
                    break outer;
            }
            mTempBuf[index++] = b;
        }
        if (doReset)
        {
            p.reset();
        }
        return new String(mTempBuf, 0, index);
    }

    int lastIndexOf(ByteBuffer p, char c)
    {
        int index = -1;
        if (p == null || p.remaining() == 0)
        {
            return -1;
        }
        p.mark();
        int record = 0;
        while (p.remaining() > 0)
        {
            byte b = p.get();
            if (b == c)
            {
                index = p.position();
            }
        }
        p.reset();
        return index;
    }

    Object responseString2(ByteBuffer p)
    {// '^...'
        String str = null;
        if (p == null || p.remaining() < 1)
        {
            return str;
        }
        p.get(); // skip "'"
        int currentIndex = p.position();
        int lastIndex = lastIndexOf(p, '\'');
        if (lastIndex != -1)
        {
            int size = lastIndex - currentIndex - 1;
            byte[] buf = new byte[size];
            for (int i = 0; i < size; i++)
            {
                buf[i] = p.get(i + currentIndex);
            }
            p.position(lastIndex);
            str = new String(buf);
        }
        return str;
    }


    Object responseBtAddress(ByteBuffer p)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++)
        {
            builder.append((char) p.get());
            builder.append((char) p.get());
            if (i != 5)
            {
                builder.append(':');
            }
        }
        return builder.toString();
    }

    public static/* private */String retToString(Object ret)
    {
        if (ret == null)
        {
            return "";
        }

        StringBuilder sb;
        String s;
        int length;
        if (ret instanceof byte[])
        {
            byte[] byteArray = (byte[]) ret;
            length = byteArray.length;
            sb = new StringBuilder("{");
            if (length > 0)
            {
                int i = 0;
                sb.append(byteArray[i++]);
                while (i < length)
                {
                    sb.append(", ").append(byteArray[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        }
        else if (ret instanceof int[])
        {
            int[] intArray = (int[]) ret;
            length = intArray.length;
            sb = new StringBuilder("{");
            if (length > 0)
            {
                int i = 0;
                sb.append(intArray[i++]);
                while (i < length)
                {
                    sb.append(", ").append(intArray[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        }
        else if (ret instanceof String[])
        {
            String[] strings = (String[]) ret;
            length = strings.length;
            sb = new StringBuilder("{");
            if (length > 0)
            {
                int i = 0;
                sb.append(strings[i++]);
                while (i < length)
                {
                    sb.append(", ").append(strings[i++]);
                }
            }
            sb.append("}");
            s = sb.toString();
        }
        else if (ret instanceof Collection)
        {
            Collection collections = (Collection) ret;
            sb = new StringBuilder("{");
            Iterator iterator = collections.iterator();
            while (iterator.hasNext())
            {
                sb.append(iterator.next()).append(", ");
            }
            sb.append("}");
            s = sb.toString();
        }
        else
        {
            s = ret.toString();
        }
        return s;
    }

    private void apujLog(String msg)
    {
        WLog.d(LOG_TAG, msg);
    }

    private void unsljLog(int responseId)
    {
        apujLog("[BT]< " + BTConstants.responseToString(responseId));
    }

    private void unsljLogMore(int responseId, String more)
    {
        apujLog("[BT]< " + BTConstants.responseToString(responseId) + " " + more);
    }

    private void unsljLogRet(int responseId, Object ret)
    {
        apujLog("[BT]< " + BTConstants.responseToString(responseId) + " " + retToString(ret));
    }

    class APUSender extends Handler implements Runnable
    {
        public APUSender(Looper looper)
        {
            super(looper);
        }

        // ***** Runnable implementation
        public void run()
        {
            // setup if needed
        }

        // ***** Handler implementation

        public void handleMessage(Message msg)
        {
            BTRequest rr = (BTRequest) (msg.obj);
            BTRequest req = null;

            switch (msg.what)
            {
                case EVENT_SEND:
                    /**
                     * mRequestMessagePending++ already happened for every
                     * EVENT_SEND, thus we must make sure mRequestMessagePending--
                     * happens once and only once
                     */
                    boolean alreadySubtracted = false;
                    try
                    {
                        if (mFileDescriptor == null)
                        {
                            rr.onError(BTConstants.BT_NOT_AVAILABLE, null);
                            rr.release();
                            mRequestMessagesPending--;
                            alreadySubtracted = true;
                            return;
                        }
                        if (rr.isSendSync())
                        {
                            synchronized (mRequestsList)
                            {
                                mRequestsList.offer(rr);
                            }
                        }

                        mRequestMessagesPending--;
                        alreadySubtracted = true;

                        byte[] data = new byte[rr.mBuffer.position() + 1];
                        System.arraycopy(rr.mBuffer.array(), 0, data, 0, data.length);
                        data[data.length - 1] = 0x0D; // <CR>

                        rr.mBuffer = null;

                        if (data.length > APU_MAX_COMMAND_BYTES)
                        {
                            throw new RuntimeException(
                                    "Jon: [BT] Parcel larger than max bytes allowed! " + data.length);
                        }

                        nativeWrite(data, 0, data.length, mFileDescriptor);
                        String[] ls = new String(data).split("\\n");
                        for (int i = 0; i < ls.length; i++)
                        {
                            WLog.d(LOG_TAG, "[BT] > " + ls[i]);
                        }
                        WLog.v("rr.isSendSync() = " + rr.isSendSync());
                        if (!rr.isSendSync())
                        {
                            WLog.v("rr.mResult() = " + rr.mResult);
                            if (rr.mResult != null)
                            {
                                AsyncResult.forMessage(rr.mResult, true, null);
                                rr.mResult.sendToTarget();
                            }
                            rr.release();
                            isReqOncompleted = true;
                        }
                    }
                    catch (Exception ex)
                    {
                        WLog.e(LOG_TAG, "Jon: [BT] IOException", ex);
                        req = removeFirstRequestFromList();
                        // make sure this request has not already been handled,
                        // eg, if APUReceiver cleared the list.
                        if (req != null || !alreadySubtracted)
                        {
                            rr.onError(BTConstants.BT_NOT_AVAILABLE, null);
                            rr.release();
                        }
                    }

                    if (!alreadySubtracted)
                    {
                        mRequestMessagesPending--;
                    }

                    break;

                case EVENT_WAKE_LOCK_TIMEOUT:
                    // Haven't heard back from the last request. Assume we're
                    // not getting a response and release the wake lock.
                    synchronized (mWakeLock)
                    {
                        if (mWakeLock.isHeld())
                        {
                            if (LOGD)
                            {
                                synchronized (mRequestsList)
                                {
                                    int count = mRequestsList.size();
                                    sTimeoutCount += count;
                                    WLog.d(LOG_TAG,
                                            "Jon: [BT] WAKE_LOCK_TIMEOUT " + " mReqPending=" + mRequestMessagesPending + " mRequestList=" + count);

                                    Iterator<BTRequest> iterator = mRequestsList.iterator();
                                    while (iterator.hasNext())
                                    {
                                        rr = (BTRequest) iterator.next();
                                        rr.onError(BTConstants.TIME_OUT, null);
                                        rr.release();
                                    }
                                    mRequestsList.clear();
                                }
                            }
                            mWakeLock.release();
                        }
                    }
                    resetIfNeeded();

                    break;
            }
        }

    }

    @Override
    public void sendCommand(String atCommand, Message result)
    {
        if ("AT*UDISKUPDATE".equals(atCommand))
        {
            startUpdate();
            return;
        }

        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_SEND_CMD, result);
        rr.mBuffer.put(atCommand.getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + atCommand);
        }
        sendAsyc(rr);
    }

    void resetIfNeeded()
    {
        if (sTimeoutCount < MAX_COMMAND_FAILURE_COUNT)
        {
            return;
        }

        if (sTimeoutCount == MAX_COMMAND_FAILURE_COUNT)
        {
            ping();
            return;
        }
        // PING is send and still TIMEOUT, so reset BT module
        resetBtModule();
        return;
    }

    private void resetBtModule()
    {
        // TODO reset bt module
        if (mIsUpdate)
        {
            return;
        }
        WLog.w(LOG_TAG, "=================BT MODULE RESET TO BE DONE HERE ==================");
        sTimeoutCount = 0;
        BTRequest.resetSerial();
        resetSerial();
    }

    @Override
    public void close()
    {
        try
        {
            if (mFileDescriptor != null)
            {
                nativeClose(mFileDescriptor);
            }
        }
        catch (Exception e)
        {
            WLog.w(LOG_TAG, "", e);
        }
    }

    void resetSerial()
    {
        WLog.d(LOG_TAG, "Jon: [BT] resetSerial Enter");
        if (mIsUpdate)
        {
            return;
        }
        close();
        try
        {
            WLog.d(LOG_TAG, "Jon: [BT] reOpen");
            mFileDescriptor = nativeOpen();
            mRequestSmsLoading = false;
            if (mFileDescriptor == null)
            {
                throw new IOException("nativeOpen failed.");
            }
            else
            {
                synchronized (mNativeFileLock)
                {
                    mNativeFileLock.notifyAll();
                }
            }
        }
        catch (Exception e)
        {
            // ignore
        }
    }

    /* default */void ping()
    {
    }

    @Override
    public void enable(Message result)
    {
        if (mIsUpdate)
        {
            return;
        }
        try
        {
            if (mFileDescriptor != null)
            {
                nativeClose(mFileDescriptor);
                mFileDescriptor = null;
            }
            mFileDescriptor = nativeOpen();
            if (mFileDescriptor == null)
            {
                AsyncResult.forMessage(result, false, null);
                result.sendToTarget();
            }
            else
            {
                synchronized (mNativeFileLock)
                {
                    mNativeFileLock.notifyAll();
                }
            }
            mRequestSmsLoading = false;
            AsyncResult.forMessage(result, true, null);
            result.sendToTarget();
        }
        catch (Exception ex)
        {
            WLog.e(LOG_TAG, "Jon: Couldn't open device");
        }
    }

    @Override
    public void disable(Message result)
    {
        if (mIsUpdate)
        {
            return;
        }
        try
        {
            if (mFileDescriptor != null)
            {
                nativeClose(mFileDescriptor);
                mFileDescriptor = null;
                synchronized (mBufLock)
                {
                    mRingBuffer.reset();
                }
                mRequestQ.reset();
                mRequestQ_L.reset();
            }
            mRequestSmsLoading = false;
            AsyncResult.forMessage(result, true, null);
            result.sendToTarget();
        }
        catch (Exception ex)
        {
            WLog.e(LOG_TAG, "Jon: Couldn't close device");
        }
    }

    @Override
    public void getVersion(Message result)
    {
        btGVER(result);
    }

    @Override
    public void getName(Message result)
    {
        btGLDN(result);
    }

    @Override
    public void setName(String name, Message result)
    {
        btSLDN(name, result);
    }

    @Override
    public void getRemoteName(String address, Message result)
    {
        btGRDN(address, result);
    }

    @Override
    public void setDiscoverable(boolean state, Message result)
    {
        btSCAN(state, result);
    }

    @Override
    public void setProfileSupported(int mask, Message result)
    {
        btSPRO(mask, result);
    }


    @Override
    public void pullPhoneBook(int storage, int type, int maxlist, int offset, Message result)
    {
        btPBCPULLPB(storage, type, maxlist, offset, result);
    }

    @Override
    public void getMessageList(int fold, int maxlist, int offset, Message result)
    {
        btMAPCGETML(fold, maxlist, offset, result);
    }

    @Override
    public void getMessageListCont(Message result)
    {
        btMAPCGETCONT(result);
    }

    @Override
    public void getMessageListCmt(Message result)
    {
        btMAPCCMT(result);
    }

    @Override
    public void getMessage(String handle, Message result)
    {
        btMAPCGETMSG(handle, result);
    }

    @Override
    public void sendMessage(Bundle b, Message result)
    {
        int more = b.getInt("more", 0);
        String message = b.getString("message");
        btMAPCPUSHMSG(more, message, result);
    }

    @Override
    public void finishPullPhoneBook(Message result)
    {
        btPBCPULLCMT(result);
    }

    @Override
    public void getCallInformation(Message result)
    {
        btHFCLCC(result);
    }

    @Override
    public void multiCallControl(int action, Message result)
    {
        btHFMCAL(action, result);
    }

    @Override
    public void setADCConfiguration(int mode, int value, Message result)
    {
    }

    @Override
    public void setInquiry(boolean state, Message result)
    {
        btINQU(state, result);
    }

    public void setAudioCodeSetting(int inCodec, int outCodec, Message result)
    {
    }

    @Override
    public void setAudioVolume(int type, int volume, Message result)
    {
    }

    @Override
    public void getAudioVolume(int type, Message result)
    {
    }

    @Override
    public void getAudioVolumeRange(int type, Message result)
    {
    }

    @Override
    public void setBond(String btBtDeviceId, int paringState, String passkey, Message result)
    {
        createBond(btBtDeviceId, paringState, passkey, result);
    }

    @Override
    public void setSimplePairMode(boolean enable, Message result)
    {
    }

    @Override
    public void setPairingMode(int pairingMode, String pinCode, Message result)
    {
    }

    @Override
    public void deletePairedDevices(String address, Message result)
    {
        btDPRD(address, result);
    }

    @Override
    public void requestUntrustedList(Message result)
    {
    }

    @Override
    public void requestTrustedList(Message result)
    {
        //btGPRD(result);
        btGPRL(result);
    }

    @Override
    public void call(String number, Message result)
    {
        btHFDIAL(0, number, result);
    }

    @Override
    public void callLastNumber(Message result)
    {
        btHFDIAL(1, null, result);
    }

    @Override
    public void receiveIncomingCall(Message result)
    {
        btHFANSW(result);
    }

    @Override
    public void playerAction(int playerAction, Message result)
    {
        switch (playerAction)
        {
            case BTConstants.BT_ACTION_PLAYER_RESUME:
                btAVRCPPLAY(result);
                break;
            case BTConstants.BT_ACTION_PLAYER_PAUSE:
                btAVRCPPAUSE(result);
                break;
            case BTConstants.BT_ACTION_PLAYER_STOP:
                btAVRCPSTOP(result);
                break;
            case BTConstants.BT_ACTION_PLAYER_NEXT:
                btAVRCPFORWARD(result);
                break;
            case BTConstants.BT_ACTION_PLAYER_PREVIOUS:
                btAVRCPBACKWARD(result);
                break;
            default:
                break;
        }
    }

    @Override
    public void playerStatus(Message result)
    {
    }

    @Override
    public void setMuteState(boolean flag, Message result)
    {
    }

    @Override
    public void getMediaMetaData(int field, Message result)
    {
    }

    @Override
    public void setRepeatMode(int repeateMode, Message result)
    {
    }

    @Override
    public void getCallState(Message result)
    {
    }

    @Override
    public void hangUpCall(int callIndex, int callCount, Message result)
    {
        btHFCHUP(result);
    }

    @Override
    public void setDeviceConnected(String address, int profile, boolean state, Message result)
    {
        WLog.d(LOG_TAG, "setDeviceConnected,address:" + address + ",profile:" + profile + ",state:" + state);
        switch (profile)
        {
            case BTConstants.BT_HFP:
                if (state)
                {
                    btHFCONN(address, result);
                }
                else
                {
                    btHFDISC(result);
                }
                break;
            case BTConstants.BT_A2DP:
                if (state)
                {
                    btA2DPCONN(address, result);
                }
                else
                {
                    btA2DPDISC(result);
                }
                break;
            case BTConstants.BT_PBAP:
                if (state)
                {
                    btPBCCONN(address, result);
                }
                else
                {
                    btPBCDISC(result);
                }
                break;
            case BTConstants.BT_HID:
                if (state)
                {
                    btHIDCONN(address, result);
                }
                else
                {
                    btHIDDISC(result);
                }
                break;
            case BTConstants.BT_MAP:
                if (state)
                {
                    btMAPCCONN(address, result);
                }
                else
                {
                    btMAPCDISC(result);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void getRepeatMode(Message result)
    {

    }

    @Override
    public void selectMicphone(int micNo, Message result)
    {
    }

    @Override
    public void setPhonePrivateState(int mode, Message result)
    {
        btHFCTRS(result);
    }

    @Override
    public void getPhonePrivateState(Message result)
    {
    }

    @Override
    public void setMicMuteState(int unMuted, Message result)
    {// PMMU
        if (unMuted == 1)
        {
            btHFMUTE(0, result);
        }
        else
        {
            btHFMUTE(1, result);
        }
    }

    @Override
    public void getMicMuteState(Message result)
    {
    }

    @Override
    public void generateDTMF(char value, Message result)
    {
        btHFDTMF(value, result);
    }

    @Override
    public void switchCalls(Message result)
    {
    }

    @Override
    public void setPBDownloadStatus(int enabled, Message result)
    {// PCDD?
    }

    @Override
    public void setDTMFEnabledAD(int enabled, Message result)
    {// PCDD?
    }

    @Override
    public void getDTMFEnabledAD(Message result)
    {
    }


    @Override
    public void setEnableMultiSync(int enabled, Message result)
    {
    }

    @Override
    public void setAutoPBSyncEnabled(int enabled, Message result)
    {
    }

    @Override
    public void getAutoConnMode(Message result)
    {
    }

    @Override
    public void setAutoConnMode(int mode, Message result)
    {
    }


    private String convertMagicChar(String content)
    {
        if (content == null)
        {
            return null;
        }
        byte[] buf = content.getBytes();
        for (int i = 0; i < buf.length; i++)
        {
            // WLog.d(TAG,"JonMM: "+buf[i]);
            if (buf[i] == 0x1A)
            {
                buf[i] = 0x0A;
            }
            else if (buf[i] == 0x1D)
            {
                buf[i] = 0x0D;
            }
        }
        return new String(buf);
    }

    // chinese_char_len: 3 means UTF-8 code, sum :means the number of the split
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

    // ////////////////////////////////////////////////////////////////////
    String toBDAddress(String mac)
    {
        try
        {
            if (mac != null)
            {
                return mac.replaceAll(":", "");
            }
        }
        catch (Exception e)
        {
        }
        return "";
    }

    boolean isMatchedResponse(BTRequest rr, String response)
    {
        if ("ERROR".equals(response))
        {
            return true;
        }
        switch (rr.mRequest)
        {
            case BTConstants.BT_REQUEST_SEND_CMD:
            {
                return true;
            }
            case BTConstants.BT_REQUEST_GVER:
            {
                if ("GVER".equals(response))
                {
                    return true;
                }
                return false;
            }
            case BTConstants.BT_REQUEST_SLDN:
            {
                if ("SLDN".equals(response))
                {
                    return true;
                }
                return false;

            }
            case BTConstants.BT_REQUEST_GPRD:
            {
                if ("GPRD".equals(response))
                {
                    return true;
                }
                return false;

            }
            case BTConstants.BT_REQUEST_SCAN:
            {
                if ("SCAN".equals(response))
                {
                    return true;
                }
                return false;

            }
            case BTConstants.BT_REQUEST_SPRO:
            {
                if ("SPRO".equals(response))
                {
                    return true;
                }
                return false;

            }
            case BTConstants.BT_REQUEST_HFCTRS:
            {
                if ("HFCTRS".equals(response))
                {
                    return true;
                }
                return false;

            }
            default:
            {
                return false;
            }
        }
    }

    Object handleReqSendCmd(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqSendCmd Enter,cmd is:" + cmd);
        return null;
    }

    Object handleReqGVER(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqGVER Enter,cmd is:" + cmd);
        String version = responseWord(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqGVER Enter,version is:" + version);
        return version;
    }

    Object handleReqGLBD(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqGLBD Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqGLBD Enter,status is:" + status + ",mac is:" + mac);
        return null;
    }

    Object handleReqGLDN(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqGLDN Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String name = responseWord(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqGLBD Enter,status is:" + status + ",name is:" + name);
        return name;
    }

    Object handleReqSLDN(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqSLDN Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqSLDN Enter,status is:" + status);
        return (status == 0);
    }

    Object handleReqGRDN(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqGRDN Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        byteBuffer.get();
        String name = responseWord(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqGRDN Enter,status is:" + status + ",mac is:" + mac + ",name is:" + name);
        return name;
    }

    Object handleReqSPIN(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqSPIN Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqSPIN Enter,status is:" + status);
        return null;
    }

    Object handleReqGPIN(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqGPIN Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String pin = responseWord(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqGPIN Enter,status is:" + status + ",pin is:" + pin);
        return null;
    }

    Object handleReqGPRD(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqGPRD Enter,cmd is:" + cmd);
/*		int total = 0;
        int index = 0;
		ArrayList<BtDeviceRemote> deviceList = new ArrayList<BtDeviceRemote>();
		do {
			total = responseUnsignedInt(byteBuffer);
			byteBuffer.get();
			index = responseUnsignedInt(byteBuffer);
			byteBuffer.get();
			String address = (String) responseBtAddress(byteBuffer);
			WLog.d(LOG_TAG, "JonReq: handleReqGPRD Enter,total is:" + total
					+ ",index is:" + index + ",address is:" + address);
			BtDeviceRemote device = new BtDeviceRemote(address);
			device.setBondState(BluetoothDevice.BOND_BONDED);
			deviceList.add(device);
			byteBuffer = readLine();
			String command = getCommand(byteBuffer);
			byteBuffer.get();
			WLog.d(LOG_TAG, "JonReq: handleReqGPRD Enter,new line command is:"
					+ command);
		} while ((total > 0) && (index < total - 1));
		return deviceList;*/
        return null;
    }

    Object handleReqDPRD(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqDPRD Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqDPRD Enter,status is:" + status + ",mac is:" + mac);
        return null;
    }

    Object handleReqINQU(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqINQU Enter,cmd is:" + cmd);
        // UNDO
        return null;
    }

    Object handleReqPAIR(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqPAIR Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqPAIR Enter,status is:" + status + ",mac is:" + mac);
        return null;
    }

    Object handleReqSCAN(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqSCAN Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqSCAN Enter,status is:" + status);
        return (status == 0);
    }

    Object handleReqEDFU(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqEDFU Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqEDFU Enter,status is:" + status);
        return null;
    }

    Object handleReqUART(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqUART Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqUART Enter,status is:" + status);
        return null;
    }

    Object handleReqSCOD(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqSCOD Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqSCOD Enter,status is:" + status);
        return null;
    }

    Object handleReqGCOD(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqGCOD Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String cod = responseWord(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqGCOD Enter,status is:" + status + ",cod is:" + cod);
        return null;
    }

    Object handleReqSPRO(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqSPRO Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqSPRO Enter,status is:" + status);
        return (status == 0);
    }

    Object handleReqHFCONN(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFCONN Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        int profile = -1;
        if (byteBuffer.remaining() > 0)
        {
            byteBuffer.get();
            profile = responseUnsignedInt(byteBuffer);
        }
        WLog.d(LOG_TAG,
                "JonReq: handleReqHFCONN Enter,status is:" + status + ",mac is:" + mac + ",profile is:" + profile);
        Bundle b = new Bundle();
        b.putInt("status", status);
        b.putString("bd", mac);
        b.putInt("profile", profile);
        return b;
    }

    Object handleReqHFDISC(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFDISC Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFDISC Enter,status is:" + status + ",mac is:" + mac);
        return null;
    }

    Object handleReqHFANSW(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFANSW Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFDISC Enter,status is:" + status);
        return null;
    }

    Object handleReqHFCHUP(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFCHUP Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFCHUP Enter,status is:" + status);
        return null;
    }

    Object handleReqHFDIAL(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFDIAL Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int type = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFCHUP Enter,status is:" + status + ",type is:" + type);
        return (status == 0);
    }

    Object handleReqHFDTMF(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFDTMF Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFDTMF Enter,status is:" + status);
        return (status == 0);
    }

    Object handleReqHFCTRS(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFCTRS Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFCTRS Enter,status is:" + status);
        return (status == 0);
    }

    Object handleReqHFMCAL(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFMCAL Enter,cmd is:" + cmd);
        // UNDO
        return null;
    }

    Object handleReqHFCLCC(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFCLCC Enter,cmd is:" + cmd);
        // UNDO
        return null;
    }

    Object handleReqHFSVGS(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFSVGS Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int vol = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFSVGS Enter,status is:" + status + ",vol is:" + vol);
        return null;
    }

    Object handleReqHFGVGS(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFGVGS Enter,cmd is:" + cmd);
        int vol = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFGVGS Enter,vol is:" + vol);
        return null;
    }

    Object handleReqHFSVGM(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFSVGM Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int vol = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFSVGM Enter,status is:" + status + ",vol is:" + vol);
        return null;
    }

    Object handleReqHFGVGM(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFGVGM Enter,cmd is:" + cmd);
        int vol = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFGVGM Enter,vol is:" + vol);
        return null;
    }

    Object handleReqHFMUTE(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFMUTE Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFMUTE Enter,status is:" + status);
        return (status == 0);
    }

    Object handleReqHFSCFG(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFSCFG Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFSCFG Enter,status is:" + status);
        return null;
    }

    Object handleReqHFGCFG(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handleReqHFGCFG Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int config = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqHFGCFG Enter,status is:" + status + ",config is:" + config);
        return null;
    }

    Object handleReqAVRCPPLAY(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "Jon: [BT] JonReq: handleReqAVRCPPLAY Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqAVRCPPLAY Enter,status is:" + status);
        return (status == 0);
    }

    Object handleReqAVRCPPAUSE(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "Jon: [BT] JonReq: handleReqAVRCPPAUSE Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqAVRCPPAUSE Enter,status is:" + status);
        return (status == 0);
    }

    Object handleReqAVRCPSTOP(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "Jon: [BT] JonReq: handleReqAVRCPSTOP Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqAVRCPSTOP Enter,status is:" + status);
        return (status == 0);
    }

    Object handleReqAVRCPFORWARD(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "Jon: [BT] JonReq: handleReqAVRCPFORWARD Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqAVRCPFORWARD Enter,status is:" + status);
        return (status == 0);
    }

    Object handleReqAVRCPBACKWARD(BTRequest req, String cmd, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "Jon: [BT] JonReq: handleReqAVRCPBACKWARD Enter,cmd is:" + cmd);
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handleReqAVRCPBACKWARD Enter,status is:" + status);
        return (status == 0);
    }


    // ///////////////////////////////////////////////////////////////////////////////////
    void handlerUnsolidRespINIT(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespINIT Enter") ;
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespINIT Enter,status is:" + status);
        boolean ret = (boolean) (status == 0);
        if (mBtEnableStateChangedRegistrant != null)
        {
            mBtEnableStateChangedRegistrant.notifyRegistrant(new AsyncResult(null, ret, null));
        }
        if (!ret)
        {
            mRequestQ.reset();
            mRequestQ_L.reset();
            mRingBuffer.reset();
        }
    }

    void handlerUnsolidRespROLE(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespROLE Enter");
    }

    void handlerUnsolidRespPAIR(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPAIR Enter");
        int result = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String address = (String) responseBtAddress(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPAIR Enter,result is:" + result + ",address is:" + address);

        Bundle b = new Bundle();
        b.putInt("result", result);
        b.putString("bd", address);
        if (mParingResponseRegistrant != null)
        {
            mParingResponseRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespHFSTAT(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespHFSTAT Enter");
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespHFSTAT Enter,status is:" + status);
        Bundle b = new Bundle();
        b.putInt("status", status);

        if (mBtHFPStateChangedRegistrant != null)
        {
            mBtHFPStateChangedRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespHFCONN(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespHFCONN Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        int profile = -1;
        if (byteBuffer.remaining() > 0)
        {
            byteBuffer.get();
            profile = responseUnsignedInt(byteBuffer);
        }
        WLog.d(LOG_TAG,
                "JonReq: handlerUnsolidRespHFCONN Enter,status is:" + status + ",mac is:" + mac + ",profile is:" + profile);
        Bundle b = new Bundle();
        b.putInt("status", status);
        b.putString("bd", mac);
        b.putInt("profile", profile);
        if (mBtHFPConnectStateRegistrant != null)
        {
            mBtHFPConnectStateRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    private void handlerUnsolidRespINQC(String command, ByteBuffer byteBuffer)
    {
        if (mDeviceInquiryFinishRegistrant != null)
        {
            mDeviceInquiryFinishRegistrant.notifyRegistrant();
        }
    }

    private void handlerUnsolidRespINQR(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "handlerUnsolidRespINQR Enter");
        String mac = (String) responseBtAddress(byteBuffer);
        byteBuffer.get();
        String c = responseWord(byteBuffer);
        byteBuffer.get();
        String name = responseWord(byteBuffer);
        Bundle b = new Bundle();
        b.putString("mac", mac);
        b.putString("class", c);
        b.putString("name", name);

        WLog.d(LOG_TAG, "mac = " + mac + " c = " + c + " name = " + name);

        if (mDeviceInquiryRegistrant != null)
        {
            mDeviceInquiryRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespHFAUDIO(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "handlerUnsolidRespHFAUDIO Enter");
        int status = responseUnsignedInt(byteBuffer);
        Bundle b = new Bundle();
        b.putInt("status", status);

        if (mBtHFAudioRegistrant != null)
        {
            mBtHFAudioRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespA2DPSTAT(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespA2DPSTAT Enter");
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespA2DPSTAT Enter,status is:" + status);
        Bundle b = new Bundle();
        b.putInt("status", status);

        if (mBtA2DPStateChangedRegistrant != null)
        {
            mBtA2DPStateChangedRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespA2DPCONN(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespA2DPCONN Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespA2DPCONN Enter,status is:" + status + ",mac is:" + mac);
        Bundle b = new Bundle();
        b.putInt("status", status);
        b.putString("bd", mac);

        if (mBtA2DPConnectStateRegistrant != null)
        {
            mBtA2DPConnectStateRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespPBCSTAT(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPBCSTAT Enter");
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPBCSTAT Enter,status is:" + status);
        Bundle b = new Bundle();
        b.putInt("status", status);

        if (mBtPBAPStateChangedRegistrant != null)
        {
            mBtPBAPStateChangedRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespPBCCONN(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPBCCONN Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPBCCONN Enter,status is:" + status + ",mac is:" + mac);
        Bundle b = new Bundle();
        b.putInt("status", status);
        b.putString("bd", mac);

        if (mBtPBAPConnectStateRegistrant != null)
        {
            mBtPBAPConnectStateRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespAVRCPCONN(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespAVRCPCONN Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespAVRCPCONN Enter,status is:" + status + ",mac is:" + mac);
        Bundle b = new Bundle();
        b.putInt("status", status);
        b.putString("bd", mac);

        if (mBtAVRCPConnectStateRegistrant != null)
        {
            mBtAVRCPConnectStateRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespAVRCPPLAY(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespAVRCPPLAY Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespAVRCPPLAY Enter,status is:" + status);
        Bundle b = new Bundle();
        b.putInt("status", status);

        if (mBtAVRCPPLAYCMDStatusRegistrant != null)
        {
            mBtAVRCPPLAYCMDStatusRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespAVRCPPAUSE(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespAVRCPPAUSE Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespAVRCPPAUSE Enter,status is:" + status);
        Bundle b = new Bundle();
        b.putInt("status", status);

        if (mBtAVRCPPAUSECMDStatusRegistrant != null)
        {
            mBtAVRCPPAUSECMDStatusRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }

    }

    void handlerUnsolidRespMAPCINIT(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCINIT Enter");
    }

    void handlerUnsolidRespMAPCCONN(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCCONN Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCCONN Enter,status is:" + status + ",mac is:" + mac);
        Bundle b = new Bundle();
        b.putInt("status", status);
        b.putString("bd", mac);

        if (mBtMAPConnectStateRegistrant != null)
        {
            mBtMAPConnectStateRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }


    void handlerUnsolidRespMAPCDISC(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCDISC Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCDISC Enter,status is:" + status + ",mac is:" + mac);
        Bundle b = new Bundle();
        b.putInt("status", (status == 0) ? 1 : 0);
        b.putString("bd", mac);

        if (mBtMAPConnectStateRegistrant != null)
        {
            mBtMAPConnectStateRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespMAPCGETDATAIND(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCGETDATAIND Enter");
        int param0 = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int param1 = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int param2 = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String packet = responseAllAsWord(byteBuffer);
        if (packet.startsWith("BEGIN"))
        {
            mRequestSmsLoading = true;
            String line = null;
            do
            {
                line = responseAllAsWord(readLine());
                if (line == null)
                {
                    WLog.d(LOG_TAG, "Jon: [BT] handlerUnsolidRespMAPCGETDATAIND line == null");
                    return;
                }
                if (line.startsWith("AT-B MAPCGETDATAIND"))
                {
                    WLog.d(LOG_TAG, "Jon: [BT] line is:" + line);
                    WLog.d(LOG_TAG, "Jon: [BT] handlerUnsolidRespMAPCGETDATAIND CP +++++++++++++++++++++++");
                    ByteBuffer ret = ByteBuffer.wrap(line.getBytes());
                    String cmd = getCommand(ret);
                    WLog.d(LOG_TAG, "Jon: [BT] handlerUnsolidRespMAPCGETDATAIND cmd is:" + cmd);
                    ret.get();//remove space;
                    param0 = responseUnsignedInt(ret);
                    WLog.d(LOG_TAG, "Jon: [BT] handlerUnsolidRespMAPCGETDATAIND param0 is:" + param0);
                    ret.get();
                    param1 = responseUnsignedInt(ret);
                    WLog.d(LOG_TAG, "Jon: [BT] handlerUnsolidRespMAPCGETDATAIND param1 is:" + param1);
                    ret.get();
                    param2 = responseUnsignedInt(ret);
                    WLog.d(LOG_TAG, "Jon: [BT] handlerUnsolidRespMAPCGETDATAIND param2 is:" + param2);
                    try
                    {
                        if (ret.remaining() > 0)
                        {
                            ret.get();
                            String newLine = responseAllAsWord(ret);
                            if (newLine != null)
                            {
                                packet = packet + newLine;
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        WLog.d(LOG_TAG, "Jon: [BT] handlerUnsolidRespMAPCGETDATAIND Error");
                    }
                    WLog.d(LOG_TAG, "Jon: [BT] handlerUnsolidRespMAPCGETDATAIND CP ------------------------");
                }
                else if (line.startsWith("AT"))
                {
                    mRequestSmsLoading = false;
                }
                else
                {
                    packet = packet + line;
                }
            } while (line != null && !packet.contains("END:BMSG") && mRequestSmsLoading);
            WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCGETDATAIND CP 22222222222222222");
            mRequestSmsLoading = false;
            Bundle b = new Bundle();
            b.putInt("type", 1); //get msg
            b.putInt("listSize", param0);
            b.putInt("moreData", param1);
            b.putInt("length", param2);
            b.putString("packet", packet);
            if (mBtMAPGetDateIndRegistrant != null)
            {
                mBtMAPGetDateIndRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
            }
        }
        else
        {
            Bundle b = new Bundle();
            b.putInt("type", 0); //list
            b.putInt("listSize", param0);
            b.putInt("moreData", param1);
            b.putInt("length", param2);
            b.putString("packet", packet);
            if (mBtMAPGetDateIndRegistrant != null)
            {
                mBtMAPGetDateIndRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
            }
        }
    }

    void handlerUnsolidRespMAPCGETCMTIND(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCGETCMTIND Enter");
        if (mBtMAPGetMsgCmtRegistrant != null)
        {
            mBtMAPGetMsgCmtRegistrant.notifyRegistrant(new AsyncResult(null, true, null));
        }
    }

    void handlerUnsolidRespMAPCPUSHCONTIND(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCPUSHCONTIND Enter");
        if (mBtMAPPushMsgIndRegistrant != null)
        {
            mBtMAPPushMsgIndRegistrant.notifyRegistrant(new AsyncResult(null, true, null));
        }

    }


    void handlerUnsolidRespMAPCPUSHCMTIND(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCPUSHCMTIND Enter");
        if (mBtMAPPushMsgCmtRegistrant != null)
        {
            mBtMAPPushMsgCmtRegistrant.notifyRegistrant(new AsyncResult(null, true, null));
        }
    }

    void handlerUnsolidRespMAPCEVTIND(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMAPCEVTIND Enter");
        int more = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int packetSz = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String packet = responseAllAsWord(byteBuffer);
        if (packetSz > 0)
        {
            String line;
            do
            {
                line = null;
                ByteBuffer buf = readLine();
                if (buf != null && buf.remaining() > 2 &&
                        buf.get(0) == 0x41 /*'A'*/ &&
                        buf.get(1) == 0x54 /*'T'*/)
                {
                    String cmd = getCommand(byteBuffer);
                    boolean solicited = isSolicited(cmd);
                    if (solicited)
                    {
                        processSolicited(cmd, byteBuffer);
                        isReqOncompleted = true;
                    }
                    else
                    {
                        processUnsolicited(cmd, byteBuffer);
                    }
                }
                else
                {
                    line = responseAllAsWord(buf);
                    if (line != null)
                    {
                        packet = packet + line;
                    }
                }
            } while (line != null && !line.startsWith("AT"));
            String evt = "unknown";
            if (packet.contains("DeliverySuccess"))
            {
                evt = "DeliverySuccess";
            }
            else if (packet.contains("SendingSuccess"))
            {
                evt = "SendingSuccess";
            }
            else if (packet.contains("NewMessage") && packet.contains("inbox"))
            {
                evt = "NewMessage";
            }
            {
                String[] ls = packet.split("\\n");
                for (int i = 0; i < ls.length; i++)
                {
                    WLog.d(LOG_TAG, "Jon: MAPCEVTIND < " + ls[i]);
                }

                WLog.d(LOG_TAG, "Jon: MAPCEVTIND evt is:" + evt);
            }

            Bundle b = new Bundle();
            b.putString("event", evt);
            mBtMAPEvtIndRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }


    void handlerUnsolidRespGRDN(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespGRDN Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        byteBuffer.get();
        String name = responseWord(byteBuffer);
        WLog.d(LOG_TAG,
                "JonReq: handlerUnsolidRespGRDN Enter,status is:" + status + ",mac is:" + mac + ",name is:" + name);

        Bundle b = new Bundle();
        b.putInt("status", status);
        b.putString("bd", mac);
        b.putString("name", name);

        if (mBtRemoteNameChangedRegistrant != null)
        {
            mBtRemoteNameChangedRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespGLDN(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespGLDN Enter");
        int status = responseUnsignedInt(byteBuffer);
        String name = "";
        if (status == 0)
        {
            byteBuffer.get();
            name = responseWord(byteBuffer);
        }
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespGLDN Enter,status is:" + status + ",name is:" + name);

        Bundle b = new Bundle();
        b.putInt("status", status);
        b.putString("name", name);

        if (mBtLocalNameChangedRegistrant != null)
        {
            mBtLocalNameChangedRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespMUTE(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMUTE Enter");
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespMUTE Enter,status is:" + status);

        Bundle b = new Bundle();
        b.putInt("status", status);

        if (mBtMUTEStatusChangeRegistrant != null)
        {
            mBtMUTEStatusChangeRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespGPRL(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespGPRL Enter");
        int total = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int index = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        byteBuffer.get();
        String name = responseWord(byteBuffer);

        Bundle b = new Bundle();
        b.putInt("total", total);
        b.putInt("index", index);
        b.putString("bd", mac);
        if (name == null)
        {
            name = "";
        }
        b.putString("name", name);

        if (mBtPairedListRegistrant != null)
        {
            mBtPairedListRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespRING(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespRING Enter");
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespRING Enter,status is:" + status);

        Bundle b = new Bundle();
        b.putInt("status", status);

        if (mBtRingStatusChangeRegistrant != null)
        {
            mBtRingStatusChangeRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }

    }


    void handlerUnsolidRespGLBD(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "handlerUnsolidRespGLBD Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String mac = (String) responseBtAddress(byteBuffer);
        WLog.v(LOG_TAG, "mac = " + mac);
        if (mBDAddressRegistrant != null)
        {
            mBDAddressRegistrant.notifyRegistrant(new AsyncResult(null, mac, null));
        }
    }

    void handlerUnsolidRespHFVGSI(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespHFVGSI Enter");
        int value = responseUnsignedInt(byteBuffer);
        Bundle b = new Bundle();
        b.putInt("hfvgsi", value);
        if (mBtVGSIRegistrant != null)
        {
            mBtVGSIRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespHFCCIN(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespHFCCIN Enter");
        int status = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int call_idx = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int direction = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int mode = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int multiparty = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        int number_type = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        String number = responseWord(byteBuffer);
        WLog.d(LOG_TAG,
                "JonReq: handlerUnsolidRespHFCCIN Enter,status is:" + status + ",call_idx:" + call_idx + ",direction:" + direction + ",mode:" + mode + ",multiparty:" + multiparty + ",number_type:" + number_type + ",number" + number);

        Bundle b = new Bundle();
        b.putInt("status", status);
        b.putInt("call_idex", call_idx);
        b.putInt("direction", direction);
        b.putInt("mode", mode);
        b.putInt("multiparty", multiparty);
        b.putInt("number_type", number_type);
        b.putString("number", number);

        if (mCallStatusChangedRegistrant != null)
        {
            mCallStatusChangedRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }


    void handlerUnsolidRespPLAYSTATUS(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPLAYSTATUS Enter");
        int status = responseUnsignedInt(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPLAYSTATUS Enter,status is:" + status);
        Bundle b = new Bundle();
        b.putInt("status", status);

        if (mPlayerStatusChangedRegistrant != null)
        {
            mPlayerStatusChangedRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }


    void handlerUnsolidRespAVRCPTITLE(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespAVRCPTITLE Enter");
        String title = responseWord(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespAVRCPTITLE Enter,title is:" + title);
        Bundle b = new Bundle();
        b.putInt("mask", MediaInfo.MEDIA_METADATA_TITTLE);
        b.putString("title", title);

        if (mPlayerMetadataChangeRegistrant != null)
        {
            mPlayerMetadataChangeRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }

    void handlerUnsolidRespAVRCPARTIST(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespAVRCPARTIST Enter");
        String artist = responseWord(byteBuffer);
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespAVRCPARTIST Enter,artist is:" + artist);
        Bundle b = new Bundle();
        b.putInt("mask", MediaInfo.MEDIA_METADATA_ARTIST);
        b.putString("artist", artist);

        if (mPlayerMetadataChangeRegistrant != null)
        {
            mPlayerMetadataChangeRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
        }
    }


    void handlerUnsolidRespPBPULLDATAIND(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPBPULLDATAIND Enter");
    }

    void handlerUnsolidRespPBCPULLCMTIND(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPBCPULLCMTIND Enter");
        if (mBtPullPBCompletedRegistrant != null)
        {
            mBtPullPBCompletedRegistrant.notifyRegistrant(new AsyncResult(null, mPhoneBookArray, null));
        }
        mPhoneBookArray = null;
    }

    void handlerUnsolidRespPBCPARSEDATAIND(String command, ByteBuffer byteBuffer)
    {
        WLog.d(LOG_TAG, "JonReq: handlerUnsolidRespPBCPARSEDATAIND Enter");
        /*if(mPhoneBookArray == null)
                    mPhoneBookArray = new ArrayList<BtPBContact>();
		BtPBContact contact = new BtPBContact();
        contact.mPbSize = responseUnsignedInt(byteBuffer);
		byteBuffer.get();
		contact.mName= responseWord(byteBuffer);
		byteBuffer.get();
		contact.mType = responseUnsignedInt(byteBuffer);
		byteBuffer.get();
		contact.mTelNumber= responseWord(byteBuffer);
		if(byteBuffer.remaining() > 0) {
		    byteBuffer.get();
		    contact.mCallLogTime = responseWord(byteBuffer);
		}*/
        if (mPhoneBookArray == null)
        {
            mPhoneBookArray = new ArrayList<BtPBContact>();
        }
        BtPBContact contact = new BtPBContact();
        contact.mPbSize = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        contact.mType = responseUnsignedInt(byteBuffer);
        byteBuffer.get();
        contact.mTelNumber = responseWord(byteBuffer);
        byteBuffer.get();
        contact.mCallLogTime = responseWord(byteBuffer);
        if (byteBuffer.remaining() > 0)
        {
            byteBuffer.get();
            contact.mName = responseWord(byteBuffer);
        }

        WLog.d(LOG_TAG,
                "JonReq: handlerUnsolidRespPBCPARSEDATAIND ,pbsize is:" + contact.mPbSize + ",mName:" + contact.mName + ",mType:" + contact.mType + ",mTelNumber:" + contact.mTelNumber + ",mCallLogTime:" + contact.mCallLogTime);

        if (contact.mPbSize > 0 && contact.mType == 0 &&
                contact.mTelNumber.equals("0") &&
                contact.mCallLogTime.equals("0"))
        {
            if (mPhoneBookSizeChangeRegistrant != null)
            {
                Bundle b = new Bundle();
                b.putInt("pbsz", contact.mPbSize);
                mPhoneBookSizeChangeRegistrant.notifyRegistrant(new AsyncResult(null, b, null));
            }
        }
        else
        {
            mPhoneBookArray.add(contact);
        }
    }

    // /////////////////////////////////////////
    public void btGVER(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_GVER, result);
        rr.mBuffer.put("AT+B GVER".getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        send(rr);
    }

    public void btINQU(boolean state, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_INQU, result);
        rr.mBuffer.put("AT+B INQU 2".getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btGLDN(Message result)
    {
        WLog.d(LOG_TAG, "Jon: btGLDN  Enter");
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_GLDN, result);
        rr.mBuffer.put("AT+B GLDN".getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btSLDN(String name, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_SLDN, result);
        StringBuilder builder = new StringBuilder("AT+B SLDN");
        builder.append(" ").append(name);
        try
        {
            rr.mBuffer.put(builder.toString().getBytes("UTF-8"));
            if (LOGD)
            {
                apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + name);
            }
            send(rr);
        }
        catch (UnsupportedEncodingException e)
        {
        }
    }

    public void btSCAN(boolean state, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_SCAN, result);
        if (state)
        {
            rr.mBuffer.put("AT+B SCAN 3".getBytes());
        }
        else
        {
            rr.mBuffer.put("AT+B SCAN 2".getBytes());
        }
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + state);
        }
        send(rr);
    }

    public void btGPRD(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_GPRD, result);
        rr.mBuffer.put("AT+B GPRD".getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        send(rr);
    }


    public void btGPRL(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_GPRL, result);
        rr.mBuffer.put("AT+B GPRL".getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btGRDN(String address, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_GRDN, result);
        StringBuilder builder = new StringBuilder("AT+B GRDN");
        builder.append(" ").append(toBDAddress(address));
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + address);
        }
        sendAsyc(rr);
    }

    public void btSPRO(int mask, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_SPRO, result);
        StringBuilder builder = new StringBuilder("AT+B SPRO");
        builder.append(" ").append(mask);
        rr.mBuffer.put(builder.toString().getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + mask);
        }
        send(rr);
    }


    public void btHFCONN(String address, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HFCONN, result);
        StringBuilder builder = new StringBuilder("AT+B HFCONN");
        builder.append(" ").append(toBDAddress(address));
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + address);
        }
        sendAsyc(rr);
    }

    public void btHFDISC(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HFDISC, result);
        StringBuilder builder = new StringBuilder("AT+B HFDISC");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btHFANSW(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HFANSW, result);
        StringBuilder builder = new StringBuilder("AT+B HFANSW");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btHFCHUP(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HFCHUP, result);
        StringBuilder builder = new StringBuilder("AT+B HFCHUP");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btHFDIAL(int type, String number, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HFDIAL, result);
        StringBuilder builder = new StringBuilder("AT+B HFDIAL");
        if (type == 1)
        {
            builder.append(" ").append(type);
        }
        else
        {
            builder.append(" ").append(type);
            builder.append(",").append(number);
        }
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants
                    .requestToString(rr.mRequest) + " type:" + type + ",number:" + number);
        }
        sendAsyc(rr);
    }


    public void btHFDTMF(char key, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HFDTMF, result);
        StringBuilder builder = new StringBuilder("AT+B HFDTMF");
        builder.append(" ").append(key);
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }


    public void btHFCTRS(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HFCTRS, result);
        StringBuilder builder = new StringBuilder("AT+B HFCTRS");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        send(rr);
    }

    public void btHFMUTE(int op, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HFMUTE, result);
        StringBuilder builder = new StringBuilder("AT+B HFMUTE");
        builder.append(" ").append(op);
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " op:" + op);
        }
        sendAsyc(rr);
    }

    public void btDPRD(String address, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_GPRD, result);
        StringBuilder builder = new StringBuilder("AT+B DPRD");
        builder.append(" ").append(toBDAddress(address));
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + address);
        }
        sendAsyc(rr);
    }

    private void createBond(String btBtDeviceId, int paringState, String passkey, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_PAIR, result);
        StringBuilder builder = new StringBuilder("AT+B PAIR");
        builder.append(" ").append(toBDAddress(btBtDeviceId));
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + btBtDeviceId);
        }
        sendAsyc(rr);
    }

    public void btA2DPCONN(String address, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_A2DPCONN, result);
        StringBuilder builder = new StringBuilder("AT+B A2DPCONN");
        builder.append(" ").append(toBDAddress(address));
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + address);
        }
        sendAsyc(rr);
    }

    public void btA2DPDISC(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_A2DPDISC, result);
        StringBuilder builder = new StringBuilder("AT+B A2DPDISC");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btPBCCONN(String address, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_PBCCONN, result);
        StringBuilder builder = new StringBuilder("AT+B PBCCONN");
        builder.append(" ").append(toBDAddress(address));
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + address);
        }
        sendAsyc(rr);
    }

    public void btPBCDISC(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_PBCDISC, result);
        StringBuilder builder = new StringBuilder("AT+B PBCDISC");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btHIDCONN(String address, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HIDCONN, result);
        StringBuilder builder = new StringBuilder("AT+B HIDCONN");
        builder.append(" ").append(toBDAddress(address));
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + address);
        }
        sendAsyc(rr);
    }

    public void btHIDDISC(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HIDDISC, result);
        StringBuilder builder = new StringBuilder("AT+B HIDDISC");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }


    public void btMAPCCONN(String address, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_MAPCCONN, result);
        StringBuilder builder = new StringBuilder("AT+B MAPCCONN");
        builder.append(" ").append(toBDAddress(address));
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest) + " " + address);
        }
        sendAsyc(rr);
    }

    public void btMAPCDISC(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_MAPCDISC, result);
        StringBuilder builder = new StringBuilder("AT+B MAPCDISC");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }


    public void btAVRCPPLAY(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_AVRCPPLAY, result);
        StringBuilder builder = new StringBuilder("AT+B AVRCPPLAY");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btAVRCPPAUSE(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_AVRCPPAUSE, result);
        StringBuilder builder = new StringBuilder("AT+B AVRCPPAUSE");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btAVRCPSTOP(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_AVRCPSTOP, result);
        StringBuilder builder = new StringBuilder("AT+B AVRCPSTOP");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btAVRCPFORWARD(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_AVRCPFORWARD, result);
        StringBuilder builder = new StringBuilder("AT+B AVRCPFORWARD");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btAVRCPBACKWARD(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_AVRCPBACKWARD, result);
        StringBuilder builder = new StringBuilder("AT+B AVRCPBACKWARD");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }


    public void btPBCPULLPB(int storage, int type, int maxlist, int offset, Message result)
    {
        mPhoneBookArray = null;
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_PBCPULLPB, result);
        StringBuilder builder = new StringBuilder("AT+B PBCPULLPB");
        builder.append(" ").append(storage);
        builder.append(",").append(type);
        builder.append(",").append(maxlist);
        builder.append(",").append(offset);

        rr.mBuffer.put(builder.toString().getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btPBCPULLCONT(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_PBCPULLCONT, result);
        StringBuilder builder = new StringBuilder("AT+B PBCPULLCONT");

        rr.mBuffer.put(builder.toString().getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btPBCPULLCRT(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_PBCPULLCRT, result);
        StringBuilder builder = new StringBuilder("AT+B PBCPULLCRT");

        rr.mBuffer.put(builder.toString().getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);

    }

    public void btPBCPULLCMT(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_PBCPULLCMT, result);
        StringBuilder builder = new StringBuilder("AT+B PBCPULLCMT");

        rr.mBuffer.put(builder.toString().getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btMAPCGETML(int fold, int maxlist, int offset, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_MAPCGETML, result);
        StringBuilder builder = new StringBuilder("AT+B MAPCGETML");
        builder.append(" ").append(fold);
        builder.append(",").append(maxlist);
        builder.append(",").append(offset);

        rr.mBuffer.put(builder.toString().getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btMAPCGETCONT(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_MAPCGETCONT, result);
        StringBuilder builder = new StringBuilder("AT+B MAPCGETCONT");

        rr.mBuffer.put(builder.toString().getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btMAPCGETMSG(String handle, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_MAPCGETMSG, result);
        StringBuilder builder = new StringBuilder("AT+B MAPCGETMSG");
        builder.append(" ").append(handle);
        rr.mBuffer.put(builder.toString().getBytes());

        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btMAPCPUSHMSG(int more, String msg, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_MAPCPUSHMSG, result);
        StringBuilder builder = new StringBuilder("AT+B MAPCPUSHMSG");
        builder.append(" ").append(more);
        builder.append(",").append(msg.getBytes().length);
        builder.append(",").append(msg);
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    public void btMAPCCMT(Message result)
    {
        WLog.d(LOG_TAG, "JonXXXX btMAPCCMT Enter");
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_MAPCCMT, result);
        StringBuilder builder = new StringBuilder("AT+B MAPCCMT");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }

        sendAsyc(rr);
    }


    public void btHFCLCC(Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HFCLCC, result);
        StringBuilder builder = new StringBuilder("AT+B HFCLCC");
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }


    public void btHFMCAL(int action, Message result)
    {
        BTRequest rr = BTRequest.obtain(BTConstants.BT_REQUEST_HFMCAL, result);
        StringBuilder builder = new StringBuilder("AT+B HFMCAL");
        builder.append(" ").append(action);
        switch (action)
        {
            case BTConstants.BT_ACTION_MULTICALL_RAAH:
                builder.append(",").append(2);
                break;
            case BTConstants.BT_ACTION_MULTICALL_PAAH:
                builder.append(",").append(2);
                break;
            default:
                builder.append(",").append(0);
                break;
        }
        rr.mBuffer.put(builder.toString().getBytes());
        if (LOGD)
        {
            apujLog(rr.serialString() + "> " + BTConstants.requestToString(rr.mRequest));
        }
        sendAsyc(rr);
    }

    /////////////////////////////////////////////////////////
    static final int BT_UPDATE_STATE_IDLE = 0;
    static final int BT_UPDATE_STATE_START = 1;
    static final int BT_UPDATE_STATE_SUCCESS = 2;
    static final int BT_UPDATE_STATE_FAILED = 3;


    int mUpdateState = BT_UPDATE_STATE_IDLE;

    Thread mBtUpdateReceiverThread;
    //   BtUpdateStatusReceiver mBtUpdateReceiver;
    String btFwPath = "/mnt/udisk/btfw/host.dfu";

    //	private class BtUpdateStatusReceiver implements Runnable {
    //		BtUpdateStatusReceiver() {
    //		}

    //		public void run() {
    //			Process.setThreadPriority(Process.THREAD_GROUP_DEFAULT);
    //			WLog.d(LOG_TAG,"JonU: BtUpdateStatusReceiver Enter");
    //			try {
    //				//delay 1s
    //				try {
    //					Thread.sleep(1000);
    //				} catch (InterruptedException er) {
    //				}
    //				SystemProperties.set("persist.sys.dfu.status", "1");//start
    //				int timeOut = 3600; //one hour
    //			    do {
    //					int percent = SystemProperties.getInt("persist.sys.dfu.process", 0);
    //					notifySendFileProcess(btFwPath,percent);
    //				    int status = SystemProperties.getInt("persist.sys.dfu.status", 0);
    //                    if(mUpdateState != status) {
    //						WLog.d(LOG_TAG,"JonU: BtUpdateStatusReceiver status is:"+status);
    //					    setBtUpdateState(status);
    //                    }
    //					try {
    //						Thread.sleep(1000);
    //                    } catch (InterruptedException er) {
    //                    }
    //					timeOut--;
    //				} while(mUpdateState == BT_UPDATE_STATE_START && (timeOut > 0));
    //			}catch (Exception ex) {
    //			}
    //	   }
    //	}


    void setBtUpdateState(int state)
    {
        WLog.d(LOG_TAG, "JonU: state " + state);
        mUpdateState = state;
        Intent intent = new Intent("chleon.android.bt.udisk.update.status");
        intent.putExtra("STATUS", mUpdateState);
        mContext.sendBroadcast(intent);
    }

    void notifySendFileProcess(String file, int percent)
    {
        Intent intent = new Intent("chleon.android.bt.udisk.sentfile");
        intent.putExtra("FILE", file);
        intent.putExtra("PERCENT", percent);
        mContext.sendBroadcast(intent);
    }

    void startUpdate()
    {
        WLog.d(LOG_TAG, "JonU: startUpdate Enter");
        mIsUpdate = true;
        setBtUpdateState(BT_UPDATE_STATE_IDLE);
        //        SystemProperties.set("persist.sys.dfu.status", "0");
        //        SystemProperties.set("persist.sys.dfu.process", "0");
        //        mBtUpdateReceiver = new BtUpdateStatusReceiver();
        //        mBtUpdateReceiverThread = new Thread(mBtUpdateReceiver, "BtUpdateStatusReceiver");
        //        mBtUpdateReceiverThread.start();
        //        SystemService.start("btDfud");
        //        SystemProperties.set("persist.sys.dfu.status", "1");
    }
}


