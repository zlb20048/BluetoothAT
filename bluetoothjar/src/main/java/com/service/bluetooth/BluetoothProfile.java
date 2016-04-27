package com.service.bluetooth;


public class BluetoothProfile {
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

	public static final int SVC_CONNECTED = 1;
	public static final int SVC_DISCONNECTED = 0;


    public static final String PROFILE_STATE_CHANGED = "chleon.android.bluetooth.device.svc.changed";

	public static final String EXTRA_PROFILE  = "SVC";

	public static final String EXTRA_STATE  = "STATE";

}

