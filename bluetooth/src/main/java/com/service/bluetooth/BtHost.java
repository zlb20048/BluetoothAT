package com.service.bluetooth;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class BtHost
{
	int currState;
    int prevState;
    // TODO get address command not added
    String address;
    boolean isInquiring;
    int scanMode;

    public BtHost()
    {
        reset();
    }

    boolean setState(int state)
    {
		if (currState == state)
		{
			return false;
		}
        prevState = currState;
        currState = state;
        return true;
    }

    int getState()
    {
        return currState;
    }

    void reset()
    {
        currState = prevState = BluetoothManager.ERROR;
    }

    void dump(FileDescriptor fd, PrintWriter pw)
    {
        pw.println("Current BT Host state:");
        pw.println("    currState  : " + currState);
        pw.println("    prevState  : " + prevState);
        pw.println("    address    : " + address);
        pw.println("    isInquiring: " + isInquiring);
        pw.println("    scanMode   : " + scanMode);
    }
}
