package com.service.bluetooth;

public class BtPBContact
{
    private static final String TAG = "BtPBContact";
    private static final boolean DBG = true;

    public int mPbSize;
    public String mName;
    public int mType;
    public String mTelNumber;
    public String mCallLogTime;

    public BtPBContact()
    {
        mPbSize = -1;
        mName = "";
        mTelNumber = "";
        mCallLogTime = "";
        mType = -1;
    }

    public String toString()
    {
        return " mPbSize is:" + mPbSize +
                " mName is:" + mName +
                " mTelNumber is:" + mTelNumber +
                " type is:" + mType +
                " mCallLogTime is:" + mCallLogTime;
    }

    public boolean isCallLog()
    {
        if (mCallLogTime != null && mCallLogTime.length() > 0)
        {
            return false;
        }
        return true;
    }

    public String getCallLogTime()
    {
        return null;
    }
}

