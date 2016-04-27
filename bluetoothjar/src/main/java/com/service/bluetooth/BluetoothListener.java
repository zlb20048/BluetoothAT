/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.service.bluetooth;

import android.os.Handler;
import android.os.Message;

/**
 * A listener class for monitoring changes in specific telephony states
 * on the device, including call state, and others.
 * <p>
 * Override the methods for the state that you wish to receive updates for, and
 * pass your TboxListener object, along with bitwise-or of the LISTEN_
 * <p>
 * Note that access to some telephony information is
 * permission-protected. Your application won't receive updates for protected
 * information unless it has the appropriate permissions declared in
 * its manifest file. Where permissions apply, they are noted in the
 * appropriate LISTEN_ flags.
 */
public class BluetoothListener {

	/**
	 * Stop listening for updates.
	 */
	public static final int LISTEN_NONE = 0;

	/**
	 * Listen for changes to the device call state.
	 * {@more}
	 * Requires Permission: {@link android.Manifest.permission#TBOX_CALL_STATE}
	 * @see #onCallStateChanged
	 */
	public static final int LISTEN_CALL_STATE                               = 0x00000001;

	public BluetoothListener() {
	}

	/**
	 * Callback invoked when device call state changes.
	 */
	public void onCallStateChanged(int state) {
		// default implementation empty
	}

	/**
	 * The callback methods need to be called on the handler thread where
	 * this object was created.  If the binder did that for us it'd be nice.
	 */
	IBluetoothListener callback = new IBluetoothListener.Stub() {

		public void onCallStateChanged(int state) {
			Message.obtain(mHandler, LISTEN_CALL_STATE, state).sendToTarget();
		}
	};

	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			// Log.d("BluetoothListener", "what=0x" + Integer.toHexString(msg.what) + " msg=" + msg);
			switch (msg.what) {
				case LISTEN_CALL_STATE:
					BluetoothListener.this.onCallStateChanged((Integer) msg.obj);
					break;
				default:
					break;
			}
		}
	};
}
