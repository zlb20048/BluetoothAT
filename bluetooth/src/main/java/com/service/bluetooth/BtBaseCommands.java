package com.service.bluetooth;

import android.os.Handler;

public abstract class BtBaseCommands implements BtCommandInterface
{

    protected Registrant mIncomingPairingRegistrant;
    protected Registrant mDeviceInquiryRegistrant;
    protected Registrant mDeviceInquiryFinishRegistrant;
    protected Registrant mParingResponseRegistrant;
    protected Registrant mExtendedResponseRegistrant;
    protected Registrant mSDPResponseRegistrant;
    protected Registrant mPasskeyResponseRegistrant;
    protected Registrant mDeleteTrustedIdResponseRegistrant;
    protected Registrant mPlayerMetadataChangeRegistrant;
    protected Registrant mPlayerStatusChangedRegistrant;
    protected Registrant mCallStatusChangedRegistrant;
    protected Registrant mDeviceConnectStateChangedRegistrant;
    protected Registrant mBtEnableStateChangedRegistrant;
    protected Registrant mBtPowerReadyRegistrant;
    protected Registrant mBtSourceStatusRegistrant;

    ///////////////////////////////////////////////////////////////////////////
    protected Registrant mPhonebookChangeRegistrantList; //= new RegistrantList();
    protected Registrant mCallLogRegistrantList;// = new RegistrantList();
    protected Registrant mPbSyncedRegistrantList;// = new RegistrantList();
    protected Registrant mMapAccountSyncedRegistrantList;// = new RegistrantList();
    protected Registrant mSvcNotificationRegistrantList;// = new RegistrantList();
    protected Registrant mMapCacheRegistrantList;// = new RegistrantList();
    protected Registrant mMapListEndRegistrantList;// = new RegistrantList();
    protected Registrant mMapMsgRetriveRegistrantList;// = new RegistrantList();
    protected Registrant mMapFileIdRetriveRegistrantList;// = new RegistrantList();
    protected Registrant mMapMsgCtlRegistrantList;// = new RegistrantList();
    protected Registrant mMapMsgEvtRegistrantList;//= new RegistrantList();
    protected Registrant mBDAddressRegistrant;//= new RegistrantList();
    /////////////////////////////////////////////////////////////////////////


    protected Registrant mBtHFPConnectStateRegistrant; //HFP  connect state
    protected Registrant mBtHFPStateChangedRegistrant; //HFP SVC state changed

    protected Registrant mBtA2DPConnectStateRegistrant; //A2DP  connect state
    protected Registrant mBtA2DPStateChangedRegistrant; //A2DP SVC state changed

    protected Registrant mBtPBAPConnectStateRegistrant; //PBAP  connect  state
    protected Registrant mBtPBAPStateChangedRegistrant; //PBAP SVC state changed

    protected Registrant mBtMAPConnectStateRegistrant; //MAP  connect state

    protected Registrant mBtAVRCPConnectStateRegistrant; //AVRCP  connect state

    protected Registrant mBtRemoteNameChangedRegistrant; //Remote Name changed

    protected Registrant mBtLocalNameChangedRegistrant; //Local Name changed

    protected Registrant mBtPullPBCompletedRegistrant; //PULL phonebook completed

    protected Registrant mBtMAPGetDateIndRegistrant; //MAP get message data indiction

    protected Registrant mBtMAPGetMsgCmtRegistrant; //MAP get message complete indication

    protected Registrant mBtMAPPushMsgCmtRegistrant; //MAP push message complete indication

    protected Registrant mBtHFAudioRegistrant; //HF Audio incation

    protected Registrant mBtMAPPushMsgIndRegistrant; //MAP push message data indication

    protected Registrant mBtMAPEvtIndRegistrant; //MAP evt message  indication

    protected Registrant mBtMUTEStatusChangeRegistrant; //MAP evt message	indication

    protected Registrant mBtPairedListRegistrant; //Bt paired List indication

    protected Registrant mPhoneBookSizeChangeRegistrant;

    protected Registrant mBtVGSIRegistrant; //Bt volume gain signal indication

    protected Registrant mBtRingStatusChangeRegistrant;

    protected Registrant mBtAVRCPPLAYCMDStatusRegistrant; //AVRCPPLAYCMD	 state

    protected Registrant mBtAVRCPPAUSECMDStatusRegistrant; //AVRCPPAUSECMD   state

    protected Registrant mBtINQRRegistrant;

    public void setBtINQRRegistrantRequest(Handler handler, int what, Object object)
    {
        mBtINQRRegistrant = new Registrant(handler, what, object);
    }

    public void setOnIncomingPairingRequest(Handler handler, int what, Object object)
    {
        mIncomingPairingRegistrant = new Registrant(handler, what, object);
    }

    public void setOnDeviceInquiry(Handler handler, int what, Object object)
    {
        mDeviceInquiryRegistrant = new Registrant(handler, what, object);
    }

    public void setOnDeviceInquiryFinished(Handler handler, int what, Object object)
    {
        mDeviceInquiryFinishRegistrant = new Registrant(handler, what, object);
    }

    public void setOnParingResponse(Handler handler, int what, Object object)
    {
        mParingResponseRegistrant = new Registrant(handler, what, object);
    }

    public void setOnExtendedResponse(Handler handler, int what, Object object)
    {
        mExtendedResponseRegistrant = new Registrant(handler, what, object);
    }

    public void setOnSDPResponse(Handler handler, int what, Object object)
    {
        mSDPResponseRegistrant = new Registrant(handler, what, object);
    }

    public void setOnPasskeyResponse(Handler handler, int what, Object object)
    {
        mPasskeyResponseRegistrant = new Registrant(handler, what, object);
    }

    public void setOnDeleteTrustedIdResponse(Handler handler, int what, Object object)
    {
        mDeleteTrustedIdResponseRegistrant = new Registrant(handler, what, object);
    }

    public void setOnPlayerMetadataChanged(Handler handler, int what, Object object)
    {
        mPlayerMetadataChangeRegistrant = new Registrant(handler, what, object);
    }

    public void setOnPlayerStatusChanged(Handler handler, int what, Object object)
    {
        mPlayerStatusChangedRegistrant = new Registrant(handler, what, object);
    }

    public void setOnCallStatusChanged(Handler handler, int what, Object object)
    {
        mCallStatusChangedRegistrant = new Registrant(handler, what, object);
    }

    public void setDeviceConnectStateChanged(Handler handler, int what, Object object)
    {
        mDeviceConnectStateChangedRegistrant = new Registrant(handler, what, object);
    }

    public void setBtEnableStateChanged(Handler handler, int what, Object object)
    {
        mBtEnableStateChangedRegistrant = new Registrant(handler, what, object);
    }

    public void setBtPowerReady(Handler handler, int what, Object object)
    {
        mBtPowerReadyRegistrant = new Registrant(handler, what, object);
    }

    public void setBtSourceStatusChanged(Handler handler, int what, Object object)
    {
        mBtSourceStatusRegistrant = new Registrant(handler, what, object);
    }

    public void setBtHostAddressChanged(Handler handler, int what, Object object)
    {
        mBDAddressRegistrant = new Registrant(handler, what, object);
    }

    public void registerForPhonebookSyncStateChanged(Handler handler, int what, Object object)
    {
        //  mPhonebookChangeRegistrantList.add(new Registrant(handler, what, object));
        mPhonebookChangeRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterForPhonebookSyncStateChanged(Handler handler)
    {
        // mPhonebookChangeRegistrantList.remove(handler);
    }
    
    public void registerCallLogStateChanged(Handler handler, int what, Object object)
    {
        // mCallLogRegistrantList.add(new Registrant(handler, what, object));
        mCallLogRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterCallLogStateChanged(Handler handler)
    {
        // mCallLogRegistrantList.remove(handler);
    }

    public void registerPhonebookSyncedEnd(Handler handler, int what, Object object)
    {
        // mPbSyncedRegistrantList.add(new Registrant(handler, what, object));
        mPbSyncedRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterPhonebookSyncedEnd(Handler handler)
    {
        // mPbSyncedRegistrantList.remove(handler);
    }

    public void registerForMapAccountIdSyncEnd(Handler handler, int what, Object object)
    {
        // mMapAccountSyncedRegistrantList.add(new Registrant(handler, what, object));
        mMapAccountSyncedRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterMapAccountIdSyncEnd(Handler handler)
    {
        // mMapAccountSyncedRegistrantList.remove(handler);
    }

    public void registerForServiceNotification(Handler handler, int what, Object object)
    {
        // mSvcNotificationRegistrantList.add(new Registrant(handler, what, object));
        mSvcNotificationRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterServiceNotification(Handler handler)
    {
        // mSvcNotificationRegistrantList.remove(handler);
    }

    public void registerForMapCache(Handler handler, int what, Object object)
    {
        // mMapCacheRegistrantList.add(new Registrant(handler, what, object));
        mMapCacheRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterMapCache(Handler handler)
    {
        // mMapCacheRegistrantList.remove(handler);
    }

    public void registerForMsgListEnd(Handler handler, int what, Object object)
    {
        //  mMapListEndRegistrantList.add(new Registrant(handler, what, object));
        mMapListEndRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterMsgListEnd(Handler handler)
    {
        // mMapListEndRegistrantList.remove(handler);
    }

    public void registerForMsgRetriveEnd(Handler handler, int what, Object object)
    {
        // mMapMsgRetriveRegistrantList.add(new Registrant(handler, what, object));
        mMapMsgRetriveRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterMsgRetriveEnd(Handler handler)
    {
        // mMapMsgRetriveRegistrantList.remove(handler);
    }

    public void registerForFileIdRetriveEnd(Handler handler, int what, Object object)
    {
        // mMapFileIdRetriveRegistrantList.add(new Registrant(handler, what, object));
        mMapFileIdRetriveRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterFileIdRetriveEnd(Handler handler)
    {
        //  mMapFileIdRetriveRegistrantList.remove(handler);
    }

    public void registerMsgCtlEvent(Handler handler, int what, Object object)
    {
        // mMapMsgCtlRegistrantList.add(new Registrant(handler, what, object));
        mMapMsgCtlRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterMsgCtlEvent(Handler handler)
    {
        // mMapMsgCtlRegistrantList.remove(handler);
    }

    public void registerMsgEvent(Handler handler, int what, Object object)
    {
        // mMapMsgEvtRegistrantList.add(new Registrant(handler, what, object));
        mMapMsgEvtRegistrantList = new Registrant(handler, what, object);
    }

    public void unregisterMsgEvent(Handler handler)
    {
        // mMapMsgEvtRegistrantList.remove(handler);
    }

    ////////////////////////////////////////////////////////////////
    public void setHFPConnStateChanged(Handler handler, int what, Object object)
    {
        mBtHFPConnectStateRegistrant = new Registrant(handler, what, object);
    }

    public void setHFPSvcStateChanged(Handler handler, int what, Object object)
    {
        mBtHFPStateChangedRegistrant = new Registrant(handler, what, object);
    }

    public void setA2DPConnStateChanged(Handler handler, int what, Object object)
    {
        mBtA2DPConnectStateRegistrant = new Registrant(handler, what, object);
    }

    public void setA2DPSvcStateChanged(Handler handler, int what, Object object)
    {
        mBtA2DPStateChangedRegistrant = new Registrant(handler, what, object);
    }


    public void setPBAPConnStateChanged(Handler handler, int what, Object object)
    {
        mBtPBAPConnectStateRegistrant = new Registrant(handler, what, object);
    }

    public void setPBAPSvcStateChanged(Handler handler, int what, Object object)
    {
        mBtPBAPStateChangedRegistrant = new Registrant(handler, what, object);
    }


    public void setMAPConnStateChanged(Handler handler, int what, Object object)
    {
        mBtMAPConnectStateRegistrant = new Registrant(handler, what, object);
    }

    public void setAVRCPConnStateChanged(Handler handler, int what, Object object)
    {
        mBtAVRCPConnectStateRegistrant = new Registrant(handler, what, object);
    }


    public void setRemoteDevNameChanged(Handler handler, int what, Object object)
    {
        mBtRemoteNameChangedRegistrant = new Registrant(handler, what, object);
    }

    public void setLocalDevNameChanged(Handler handler, int what, Object object)
    {
        mBtLocalNameChangedRegistrant = new Registrant(handler, what, object);
    }


    public void setPullPBCmtChanged(Handler handler, int what, Object object)
    {
        mBtPullPBCompletedRegistrant = new Registrant(handler, what, object);
    }

    public void setGetMessageDataIndChanged(Handler handler, int what, Object object)
    {
        mBtMAPGetDateIndRegistrant = new Registrant(handler, what, object);
    }


    public void setPushMessageDataIndChanged(Handler handler, int what, Object object)
    {
        mBtMAPPushMsgIndRegistrant = new Registrant(handler, what, object);
    }

    public void setGetMessageCmtChanged(Handler handler, int what, Object object)
    {
        mBtMAPGetMsgCmtRegistrant = new Registrant(handler, what, object);
    }

    public void setPushMessageCmtChanged(Handler handler, int what, Object object)
    {
        mBtMAPPushMsgCmtRegistrant = new Registrant(handler, what, object);
    }

    public void setMessageEvtChanged(Handler handler, int what, Object object)
    {
        mBtMAPEvtIndRegistrant = new Registrant(handler, what, object);
    }

    public void setHFAudioStatusChanged(Handler handler, int what, Object object)
    {
        mBtHFAudioRegistrant = new Registrant(handler, what, object);
    }

    public void setMUTEStatusChanged(Handler handler, int what, Object object)
    {
        mBtMUTEStatusChangeRegistrant = new Registrant(handler, what, object);
    }

    public void setRingStatusChanged(Handler handler, int what, Object object)
    {
        mBtRingStatusChangeRegistrant = new Registrant(handler, what, object);
    }

    public void setBtPairedListChanged(Handler handler, int what, Object object)
    {
        mBtPairedListRegistrant = new Registrant(handler, what, object);
    }

    public void setBtPhoneBookSzChanged(Handler handler, int what, Object object)
    {
        mPhoneBookSizeChangeRegistrant = new Registrant(handler, what, object);
    }

    
    public void setBtVGSIChanged(Handler handler, int what, Object object)
    {
        mBtVGSIRegistrant = new Registrant(handler, what, object);
    }

    public void setAVRCPPLAYCmdStatusChanged(Handler handler, int what, Object object)
    {
        mBtAVRCPPLAYCMDStatusRegistrant = new Registrant(handler, what, object);
    }

    public void setAVRCPPAUSECmdStatusChanged(Handler handler, int what, Object object)
    {
        mBtAVRCPPAUSECMDStatusRegistrant = new Registrant(handler, what, object);
    }


}
