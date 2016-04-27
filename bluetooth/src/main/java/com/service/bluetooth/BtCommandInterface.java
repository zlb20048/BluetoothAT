package com.service.bluetooth;

import android.os.Handler;
import android.os.Message;
import android.os.Bundle;


public interface BtCommandInterface
{
    /**
     * For development purpose only.
     *
     * @hide
     */
    void sendCommand(String atCommand, Message result);

    /**
     * For development purpose only.
     *
     * @hide
     */
    void close();

    /**
     * This command is used to enable Bluetooth
     *
     * @param result is a callback message,
     *               AsyncResult.result is boolean represents the status of BT.
     */
    void enable(Message result);

    /**
     * This command is used to enable Bluetooth
     */
    void disable(Message result);

    /**
     * This command is used to get bluetooth device
     * friendly name.
     * Callback AsyncResult.result is String name.
     */
    void getName(Message result);

    /**
     * This command is used to set bluetooth device
     * friendly name. Callback is boolean.
     */
    void setName(String name, Message result);

    /**
     * This command is used to get remote bluetooth device
     * friendly name.
     * Callback AsyncResult.result is String name.
     */
    void getRemoteName(String address, Message result);


    /**
     * This command is used to start/Stop Bluetooth Discoverable mode
     *
     * @param result is a callback message,
     *               AsyncResult.result is boolean represents the status of BT.
     */
    void setDiscoverable(boolean state, Message result);

    /**
     * This command is used to configure the ADC of the module.
     */
    void setADCConfiguration(int mode, int value, Message result);

    /**
     * This command is used to start/Stop Bluetooth Inquiry mode
     *
     * @param result is a callback message,
     *               AsyncResult.result is boolean represents the status of BT.
     */
    void setInquiry(boolean state, Message result);

    /**
     * This command is used to setup Audio Codec
     *
     * @param result is a callback message,
     *               AsyncResult.result is boolean represents the status of BT.
     */
    public abstract void setAudioCodeSetting(int inCodec, int outCodec, Message result);

    /**
     * This command is used to Pair host with a bluetooth device
     *
     * @param result is a callback message,
     *               AsyncResult.result is boolean represents the status of BT.
     */
    void setBond(String mbtBtDeviceId, int paringState, String passkey, Message result);

    /**
     * sets the pairing mode of HOST.
     *
     * @param pairingMode pairing mode
     * @param pinCode     optional string pin code
     *                    AsyncResult.result is boolean represents the status of pairing Mode.
     */
    void setPairingMode(int pairingMode, String pinCode, Message result);

    /**
     * Deletes the Paired device
     *
     * @param trustedDevAddr list of trusted devices
     *                       AsyncResult.result is boolean represents the status of pairing Mode.
     */
    void deletePairedDevices(String trustedDevAddr, Message result);

    /**
     * Place a call to the specified number.
     *
     * @param number the number to be called.
     *               AsyncResult.result is boolean represents the status of call.
     */
    void call(String number, Message result);

    /**
     * Dial the last dialed number in BT connected Phone.
     * AsyncResult.result is boolean represents the status of call.
     */
    void callLastNumber(Message result);

    /**
     * Recieve the incoming call
     * AsyncResult.result is boolean represents the status of call.
     */
    void receiveIncomingCall(Message result);

    /**
     * Request for the Untrusted list .
     *
     * @param result list of trusted devices
     *               AsyncResult.result is ArrayList of may contain 0 element.
     */
    void requestUntrustedList(Message result);

    /**
     * Request for the trusted list .
     *
     * @param result list of trusted devices
     *               AsyncResult.result is ArrayList of may contain 0 element.
     */
    void requestTrustedList(Message result);

    /**
     * Performs actions related to the player (music player).
     *
     * @param playerAction to be performed on the player of connected device .
     *                     AsyncResult.result is void , Action performed on the player.
     */
    void playerAction(int playerAction, Message result);

    /**
     * Returns the status of the player.
     * AsyncResult.result is boolean value represents the status of the action performed on the player.
     */
    void playerStatus(Message result);

    /**
     * Returns true if muted/Unmuted.
     * AsyncResult.result is boolean value represents the status of the action performed on the player.
     */
    void setMuteState(boolean flag, Message result);

    /**
     * Returns metadata for media represented by <code>field</code>.
     *
     * @param field is one of
     *              {@link MediaInfo#MEDIA_METADATA_ALBUM},
     *              {@link MediaInfo#MEDIA_METADATA_ARTIST},
     *              {@link MediaInfo#MEDIA_METADATA_COMPOSER},
     *              {@link MediaInfo#MEDIA_METADATA_GENRE},
     *              {@link MediaInfo#MEDIA_METADATA_TITTLE} or
     *              {@link MediaInfo#MEDIA_METADATA_TRACK}.
     *              AsyncResult.result is {@link MediaInfo}.
     */
    void getMediaMetaData(int field, Message result);

    /**
     * Returns true if random mode is changed
     * AsyncResult.result is boolean value if the mode is set to repeate mode.
     */
    void setRepeatMode(int repeateMode, Message result);

    /**
     * Returns state of the Call
     * AsyncResult.result is state of an on going call.
     */

    void getCallState(Message result);

    /**
     * Returns true if the call is rejected
     *
     * @param callIndex the call index of the current running call.
     *                  AsyncResult.result is boolean value regarding call is disconnected or not.
     */

    void hangUpCall(int callIndex, int callCount, Message result);

    /**
     * Returns true if device sate is set sucessfully
     *
     * @param state true for connecting , false for disconnecting(Device)in both case.
     *              AsyncResult.result is boolean value regarding call is disconnected or not.
     */

    void setDeviceConnected(String address, int profile, boolean state, Message result);

    /**
     * Returns the state of current repeate mode
     * AsyncResult.result is String value current repeate mode is returned.
     */
    void getRepeatMode(Message result);


    /**
     * sets the one of two input Micphone.
     *
     * @param micNo which  micphone will choiced
     *              AsyncResult.result is boolean  if the micphone is set OK.
     */
    void selectMicphone(int micNo, Message result);

    /**
     *
     */
    void setPhonePrivateState(int mode, Message result);

    /**
     *
     */
    void getPhonePrivateState(Message result);

    /**
     *
     */

    void setMicMuteState(int unmuted, Message result);

    /**
     *
     */
    void getMicMuteState(Message result);

    /**
     *
     */
    void generateDTMF(char value, Message result);

    /**
     *
     */
    void setDTMFEnabledAD(int enabled, Message result);

    /**
     *
     */
    void getDTMFEnabledAD(Message result);

    /**
     *
     */
    void switchCalls(Message result);

    /**
     *
     */
    void setEnableMultiSync(int enabled, Message result);


    /**
     *
     */
    void setAutoPBSyncEnabled(int enabled, Message result);


    /**
     *
     */

    void setPBDownloadStatus(int enable, Message result);

    /**
     *
     */
    void setAutoConnMode(int mode, Message result);

    /**
     *
     */
    void getAutoConnMode(Message result);


    /**
     * This command is used to setup Audio Volume
     *
     * @param result is a callback message,
     *               AsyncResult.result is boolean represents the status of BT.
     */
    public abstract void setAudioVolume(int type, int volume, Message result);

    /**
     * This command is used to get Audio Volume
     *
     * @param result is a callback message,
     *               AsyncResult.result is boolean represents the status of BT.
     */
    public abstract void getAudioVolume(int type, Message result);


    /**
     * This command is used to get Audio Volume range
     *
     * @param result is a callback message,
     *               AsyncResult.result is boolean represents the status of BT.
     */
    public abstract void getAudioVolumeRange(int type, Message result);


    /**
     *
     */
    void setSimplePairMode(boolean enable, Message result);


    /**
     * This command is used to get bluetooth device
     * Version.
     * Callback AsyncResult.result is String Version.
     */
    void getVersion(Message result);


	
	/*==============================================================
	 * BELOW ARE CALLBACK METHODS ASSOCIATED TO ONLY ONE LISTENER 
	 *==============================================================*/

    /**
     * Register for event called when the incoming pairing request comes.
     *
     * @param object AsyncResult.result is not defined yet.
     */
    void setOnIncomingPairingRequest(Handler handler, int what, Object object);

    /**
     * Register for event called when the incoming pairing request comes.
     *
     * @param object AsyncResult.result is not defined yet.
     */
    void setOnDeviceInquiry(Handler handler, int what, Object object);

    /**
     * Register for event called when the device inquiry finished.
     *
     * @param object AsyncResult.result is boolean.
     */
    void setOnDeviceInquiryFinished(Handler handler, int what, Object object);

    /**
     * Register for event called when response of paring comes
     *
     * @param object AsyncResult.result is String array of size 3
     *               containing DeviceId(InquiredId-String), Status(int), pairedId(String)
     */
    void setOnParingResponse(Handler handler, int what, Object object);
    
    /**
     * Register for event called when response of Extended bluetooth information comes.
     *
     * @param object AsyncResult.result is not defined yet.
     */
    void setOnExtendedResponse(Handler handler, int what, Object object);
    
    /**
     * Register for event called when SDP response comes.
     *
     * @param object AsyncResult.result is not defined yet.
     */
    void setOnSDPResponse(Handler handler, int what, Object object);
    
    /**
     * Register for event called when passkey response comes.
     *
     * @param object AsyncResult.result is string array of size 4
     *               containing: remoteId(String), SSPmode(int), IOCapability(int) and
     *               passKey(String parsable to int) and it may be null
     */
    void setOnPasskeyResponse(Handler handler, int what, Object object);
    
    /**
     * Register for event called when Deleted from trusted list.
     *
     * @param object AsyncResult.result is string array containing
     *               deleted device's pairedIds.
     */
    void setOnDeleteTrustedIdResponse(Handler handler, int what, Object object);
    
    
    /**
     * Register for event when player's meta data changed.
     *
     * @param object AsyncResult.result is integer containing bit mask
     *               representing what field has been changed. Mask is one of
     *               {@link MediaInfo#MEDIA_METADATA_ALBUM},
     *               {@link MediaInfo#MEDIA_METADATA_ARTIST},
     *               {@link MediaInfo#MEDIA_METADATA_COMPOSER},
     *               {@link MediaInfo#MEDIA_METADATA_GENRE},
     *               {@link MediaInfo#MEDIA_METADATA_TITTLE} or
     *               {@link MediaInfo#MEDIA_METADATA_TRACK}.
     */
    void setOnPlayerMetadataChanged(Handler handler, int what, Object object);
    
    /**
     * Register for event of player status i.e. Currentposition,isPlaying,source info
     *
     * @param object AsyncResult.result is string array of size 5 if the player is not in stop mode
     */
    void setOnPlayerStatusChanged(Handler handler, int what, Object object);
    
    /**
     * Register for event of call Status.
     *
     * @param object AsyncResult.result is string array of size 5 if the player is not in stop mode
     */
    void setOnCallStatusChanged(Handler handler, int what, Object object);
    
    /**
     * Register for event of connect state.
     */
    void setDeviceConnectStateChanged(Handler handler, int what, Object object);

    /**
     * Register for event of connect state.
     */
    void setBtEnableStateChanged(Handler handler, int what, Object object);

    /**
     * Register for event of bt power ready.
     */
    void setBtPowerReady(Handler handler, int what, Object object);

    /**
     * Register for event of bt player source status changed.
     */
    void setBtSourceStatusChanged(Handler handler, int what, Object object);


    /**
     */
    void setBtHostAddressChanged(Handler handler, int what, Object object);

    ////////////////////////////////////////////////////////////////////////////////

    /**
     */
    void setHFPConnStateChanged(Handler handler, int what, Object object);

    /**
     */
    void setHFPSvcStateChanged(Handler handler, int what, Object object);

    /**
     */
    void setA2DPConnStateChanged(Handler handler, int what, Object object);

    /**
     */
    void setA2DPSvcStateChanged(Handler handler, int what, Object object);

    /**
     */
    void setPBAPConnStateChanged(Handler handler, int what, Object object);

    /**
     */
    void setPBAPSvcStateChanged(Handler handler, int what, Object object);

    /**
     */
    void setMAPConnStateChanged(Handler handler, int what, Object object);

    /**
     */
    void setAVRCPConnStateChanged(Handler handler, int what, Object object);

    /**
     */
    void setAVRCPPLAYCmdStatusChanged(Handler handler, int what, Object object);


    /**
     */
    void setAVRCPPAUSECmdStatusChanged(Handler handler, int what, Object object);


    /**
     */
    void setRemoteDevNameChanged(Handler handler, int what, Object object);

    /**
     */
    void setLocalDevNameChanged(Handler handler, int what, Object object);


    /**
     */
    void setPullPBCmtChanged(Handler handler, int what, Object object);

    /**
     */
    void setGetMessageDataIndChanged(Handler handler, int what, Object object);


    /**
     */
    void setPushMessageDataIndChanged(Handler handler, int what, Object object);

    /**
     */
    void setGetMessageCmtChanged(Handler handler, int what, Object object);

    /**
     */
    void setPushMessageCmtChanged(Handler handler, int what, Object object);

    /**
     */
    void setMessageEvtChanged(Handler handler, int what, Object object);

    /**
     */
    void setHFAudioStatusChanged(Handler handler, int what, Object object);

    /**
     */
    void setMUTEStatusChanged(Handler handler, int what, Object object);

    /**
     */
    void setBtPairedListChanged(Handler handler, int what, Object object);

    /**
     */
    void setBtVGSIChanged(Handler handler, int what, Object object);


    /**
     */
    void setBtPhoneBookSzChanged(Handler handler, int what, Object object);

    
    /**
     */
    void setRingStatusChanged(Handler handler, int what, Object object);
    /////////////////////////////////////////////////////////////////////////////////


    /**
     * Register for event of phonebook sync state.
     */
    void registerForPhonebookSyncStateChanged(Handler handler, int what, Object object);

    
    /**
     * UnRegister for event of phonebook sync state.
     */
    public abstract void unregisterForPhonebookSyncStateChanged(Handler handler);

    /**
     * Register for event of call Log state.
     */
    void registerCallLogStateChanged(Handler handler, int what, Object object);


    /**
     * UnRegister for event of  Call Log state.
     */
    public abstract void unregisterCallLogStateChanged(Handler handler);

    /**
     * Register for event of phonebook synced end
     */
    void registerPhonebookSyncedEnd(Handler handler, int what, Object object);


    /**
     * UnRegister for event of  Phonebook Synced end
     */
    public abstract void unregisterPhonebookSyncedEnd(Handler handler);


    ///////////////////////////IVT /////////////////////////////////////////

    /**
     * Bluetooth set Profile support
     */
    public abstract void setProfileSupported(int mask, Message result);

    /**
     * Bluetooth pull phone book
     */
    public abstract void pullPhoneBook(int storage, int type, int maxlist, int offset, Message result);


    /**
     * Bluetooth get message list
     */
    public abstract void getMessageList(int fold, int maxlist, int offset, Message result);

    
    /**
     * Bluetooth get message list Cont
     */
    public abstract void getMessageListCont(Message result);


    /**
     * Bluetooth get message list Cmt
     */
    public abstract void getMessageListCmt(Message result);


    /**
     * Bluetooth get message
     */
    public abstract void getMessage(String handler, Message result);

    /**
     * Bluetooth send message
     */
    public abstract void sendMessage(Bundle b, Message result);

    /**
     * Finish Bluetooth Pull PB
     */
    public abstract void finishPullPhoneBook(Message result);


    /**
     * get Call information
     */
    public abstract void getCallInformation(Message result);


    /**
     * get Call information
     */
    public abstract void multiCallControl(int action, Message result);


    ////////////////////////////////////////////////////////////////////

    /**
     * Register for event Map accountId Synced  state.
     */
    void registerForMapAccountIdSyncEnd(Handler handler, int what, Object object);

    /**
     * UnRegister for event of  Map accountId Synced  end
     */
    public abstract void unregisterMapAccountIdSyncEnd(Handler handler);

    /**
     * Register for event Bluetooth Service notifications.
     */
    void registerForServiceNotification(Handler handler, int what, Object object);

    /**
     * UnRegister for event Bluetooth Service notifications.
     */
    public abstract void unregisterServiceNotification(Handler handler);

    
    /**
     * Register for  event of  Map Cache End.
     */
    void registerForMapCache(Handler handler, int what, Object object);

    /**
     * UnRegister for event of  Map Cache End.
     */
    public abstract void unregisterMapCache(Handler handler);

    
    /**
     * Register for  event of  Map List End.
     */
    void registerForMsgListEnd(Handler handler, int what, Object object);

    /**
     * UnRegister for event of  Map List End.
     */
    public abstract void unregisterMsgListEnd(Handler handler);

    
    /**
     * Register for  event of  Map Retrive End.
     */
    void registerForMsgRetriveEnd(Handler handler, int what, Object object);

    /**
     * UnRegister for event of  Map Retrive End.
     */
    public abstract void unregisterMsgRetriveEnd(Handler handler);


    /**
     * Register for  event of  Map FileId Retrive End.
     */
    void registerForFileIdRetriveEnd(Handler handler, int what, Object object);

    /**
     * UnRegister for event of  Map FileId  Retrive End.
     */
    public abstract void unregisterFileIdRetriveEnd(Handler handler);


    /**
     * Register for  event of  Map Msg Control.
     */
    void registerMsgCtlEvent(Handler handler, int what, Object object);

    /**
     * UnRegister for event of Map Msg Control.
     */
    public abstract void unregisterMsgCtlEvent(Handler handler);

    /**
     * Register for  event of  Map Msg Event.
     */
    void registerMsgEvent(Handler handler, int what, Object object);

    /**
     * UnRegister for event of Map Msg Event.
     */
    public abstract void unregisterMsgEvent(Handler handler);

}
