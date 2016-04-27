package com.service.bluetooth;

import android.os.ParcelUuid;
import com.service.bluetooth.BluetoothMessage;
import com.service.bluetooth.IBluetoothListener;
import com.service.bluetooth.IBluetoothPbapCallback;

interface IBluetoothManager {
	/*
	 * only for testing
	 */
	void sendCommand(in String command);
	/*
	 * only for testing
	 */
	void close();

	boolean isEnabled();

    boolean bluetooth_enable(boolean on);

    /*
	*/
	boolean enable();

    /*
	*/
	boolean disable(boolean persistSetting);

	boolean setDeviceConnected(in String address, boolean state);

	int getBluetoothState();


	String getName();

	boolean setName(String name);

	String getAddress();


	int getScanMode();

	boolean setScanMode(int mode, int duration);

	boolean startDiscovery();

	boolean cancelDiscovery();

	boolean isDiscovering();

	int getMaxBondedDeviceCount();

	String[] getBondedDevices();

	boolean createBond(in String address);

	boolean cancelBondProcess(in String address);

	boolean removeBond(in String address);

	String getRemoteName(in String address);

	boolean setTrust(in String address, in boolean value);

	boolean getTrustState(in String address);


	ParcelUuid[] getRemoteUuids(in String address);

	boolean fetchRemoteUuids(in String address, in ParcelUuid uuid);

	int addRfcommServiceRecord(in String serviceName, in ParcelUuid uuid, int channel, IBinder b);

	void removeServiceRecord(int handle);

	int getBondState(in String address);


	boolean setPin(in String address, in byte[] pin);

	boolean setPairingConfirmation(in String address, boolean confirm);

	boolean cancelPairingUserInput(in String address);

	/**
	 * Place a call to the specified number.
	 * This is asynchronus procedure.
	 * @param number the number to be called.
	 */
	void call(String number);

	/**
	 * Place a call from recent call list
	 * to connected phone .
	 * This is asynchronus procedure.
	 * @param number the number to be called.
	 */
	void callLastNumber(); 

	/**
	 * Receive an incoming call
	 * This is asynchronus procedure.
	 * @param number the number to be called.
	 */
	void recieveIncomingCall();

	/**
	 * disconnect a call
	 * This is asynchronus procedure.
	 */
	void disconnectCall();

	/**
	 * reject an Incomming call
	 * This is asynchronus procedure.
	 */
	boolean rejectCall();



	int getCallState();

	int getNetworkType();

	int getSignalStrength();

	void musicResume();

	void musicStop();

	void musicPause();

	void playNextTrack();

	void playPreviousTrack(); 

	int getCurrentPosition();

	int getDuration();

	boolean isPlaying();

	String getArtist();

	String getAlbum();

	int getTrack();

	String getTittle();

	String getComposer();

	String getGenre();

	boolean setMuteState(boolean state);

	boolean setRepeateMode(int mode);

	int getRepeateMode();

	boolean muteCall(boolean state);

	int getState(in String address);

	int getPhonebookSize(in String address);

	int getA2DPSinkSvcState(in String address);

	boolean isSupportPhoneBook(in String address);

	boolean isPhoneBookSynced(in String address);

	int getPhoneBookSyncProgress(in String address);   

	int getPhonePrivateMode();

	boolean setPhonePrivateMode(in int mode);

	int getMicMuteState();

	boolean setMicMuteState(in int unMuted);

	boolean generateDTMF(in char value);

	boolean switchCalls();

	String getCallDetail();

	boolean setAudioVolume(in int type,in int value);

	int getAudioVolume(in int type);

	int[] getAudioVolumeRange(in int type);

	boolean setStartPBSyncManual(in String address);


	int getAutoConnMode();

	boolean setAutoConnMode(in int mode);

	boolean setADCConfiguration(in int type,in int value);

	List<BluetoothMessage> getBtMessage(in String address); 

	int getMapSvcState(in String address);

	int getPhoneSvcState(in String address);

	boolean isMapMsgDownloading(in String address);

	boolean retriveMapMessage(in String address,in int accountId,in int msgId);

	boolean sendMapMessage(in String address, in BluetoothMessage msg);

	String getSwVersion();

	boolean enableUpdateMode();

	void ttsSpeak(in String text);

	boolean playerStart(in String address);

	int getPhoneBookSyncedStatus(in String address);

	void getPhoneBookByManual(in String address);  

	//for Neusoft interface
	String GetConnectDeviceAddr(int profile);

	//new getConnectDeviceAddr
    String getConnectDeviceAddr(int profile);

	//for test
	int getRssi(in String address);

	//connect special profile 
	boolean deviceConnect(String address, int profile ,boolean state);

	//get specail profile svc state
	int getDeviceServiceState(String address,int profile);

	/**
	 * Register listener for Tbox.                                                                                                         
	 *
	 * @param pkg, pkgForDebug
	 * @param callback, refer to ITBoxListener for more info
	 * @param events, event apps care about
	 * @param notifyNow, whether notify immediately or not
	 */

	void listen(in String pkg, in IBluetoothListener callback, int events, boolean notifyNow);

    /**
     * @param callback
     */
	void addBluetoothPbapCallback(in IBluetoothPbapCallback callback);
}
