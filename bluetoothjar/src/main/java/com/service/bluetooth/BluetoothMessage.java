package com.service.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

public final class BluetoothMessage implements Parcelable {

	public static final int MAP_MSG_FILED_ID           = 0;
	public static final int MAP_MSG_FILED_SUBJECT      = 1;
	public static final int MAP_MSG_FILED_DATETIME     = 2;
	public static final int MAP_MSG_FILED_SENDERNAME   = 3;
	public static final int MAP_MSG_FILED_SENDADDR     = 4;
	public static final int MAP_MSG_FILED_REPLYTOADDR  = 5;
	public static final int MAP_MSG_FILED_RECIPNAME    = 6;
	public static final int MAP_MSG_FILED_RECIPADDR    = 7;
	public static final int MAP_MSG_FILED_TYPE         = 8;
	public static final int MAP_MSG_FILED_SIZE         = 9;

	public String deviceId;
	public int accountId;    
	public int msgId;           //0
	public String subject;      // 1
	public long dateTime;        // 2
	public String senderName;   // 3 
	public String senderAddr;   // 4
	public String replayToAddr; // 5
	public String recipname;    // 6 
	public String recipaddr;    // 7
	public int type;            // 8
	public int size;            // 9    
	public int fold;
	public String mHandle;

	public String content;

	public BluetoothMessage() {
		deviceId = "";
		accountId = -1;
		msgId = -1;
		subject = "";
		dateTime= -1;
		senderName = "";
		senderAddr = "";
		replayToAddr = "";
		recipname = "";
		recipaddr = "";
		mHandle = "";
		type = -1;
		size = -1;  
		fold = -1;
		content = null;
	}


	@Override
	public String toString() {
		return "BtMapMessageHeader: "+
			" deviceId:"+deviceId+
			" accountId:"+accountId+
			" msgId:"+msgId+
			" subject:"+subject+
			" dateTime:"+dateTime+
			" senderName: "+senderName+
			" senderAddr: "+senderAddr+
			" replayToAddr: "+replayToAddr+
			" recipname: "+recipname+
			" recipaddr: "+recipaddr+
			" type: "+type+
			" size: "+size+
			" fold: "+fold+
			" content:"+content+
			" mHandle:"+mHandle;
	}


	public static final Creator<BluetoothMessage> CREATOR =
		new Creator<BluetoothMessage>() {
			public BluetoothMessage createFromParcel(Parcel source) {
				BluetoothMessage message = new BluetoothMessage();
				message.deviceId  = source.readString();
				message.accountId = source.readInt();
				message.msgId = source.readInt();
				message.subject = source.readString();
				message.dateTime = source.readLong();
				message.senderName = source.readString();
				message.senderAddr = source.readString();
				message.replayToAddr = source.readString();
				message.recipname = source.readString();
				message.recipaddr = source.readString();
				message.type = source.readInt();
				message.size = source.readInt();
				message.fold = source.readInt();
				message.content = source.readString();
				message.mHandle = source.readString();
				return message;
			}

			public BluetoothMessage[] newArray(int size) {
				return new BluetoothMessage[size];
			}
		};

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(deviceId);
		dest.writeInt(accountId);
		dest.writeInt(msgId);
		dest.writeString(subject);
		dest.writeLong(dateTime);
		dest.writeString(senderName);
		dest.writeString(senderAddr);
		dest.writeString(replayToAddr);
		dest.writeString(recipname);
		dest.writeString(recipaddr);
		dest.writeInt(type);
		dest.writeInt(size);
		dest.writeInt(fold);
		dest.writeString(content);
		dest.writeString(mHandle);
	}
}

