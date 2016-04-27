package com.service.bluetooth;

import java.io.FileDescriptor;
import java.io.PrintWriter;


public class CallStatus
{
    int callIndex;
    String phoneNumber, callerId;
    int callState, callDir, callConf, callMode, pnType;
    long activeStartTime;

    CallStatus()
    {
        phoneNumber = callerId = null;
        callState = BTConstants.BT_CALL_STATE_FREE;
        callIndex = callDir = callConf = callMode = pnType = -1;
        activeStartTime = -1;
    }

    boolean setCallState(int state)
    {
        if (callState == state)
        {
            return false;
        }
        callState = state;
        return true;
    }

    boolean setCallId(String callId)
    {
        if ((callId == null) || callId.equals(callerId))
        {
            return false;
        }
        callerId = callId;
        return true;
    }


    @Override
    public String toString()
    {
        return "CallStatus [callIndex=" + callIndex + ", phoneNumber=" + phoneNumber + ", callerId=" + callerId + ", callState=" + callState + ", callDir=" + callDir + ", callConf=" + callConf + ", callMode=" + callMode + ", pnType=" + pnType + ", activeStartTime=" + activeStartTime + "]";
    }

    void reset()
    {
        phoneNumber = callerId = null;
        callState = BTConstants.BT_CALL_STATE_FREE;
        callIndex = callDir = callConf = callMode = pnType = -1;
        activeStartTime = -1;
    }

    void dump(FileDescriptor fd, PrintWriter pw)
    {
        pw.println("Current CallStatus Info state:");
        pw.println("     callConf : " + callConf);
        pw.println("     callDir : " + callDir);
        pw.println("     callerId : " + callerId);
        pw.println("     callIndex : " + callIndex);
        pw.println("     callMode : " + callMode);
        pw.println("     callState : " + callState);
        pw.println("     phoneNumber : " + phoneNumber);
        pw.println("     pnType : " + pnType);
    }
}
