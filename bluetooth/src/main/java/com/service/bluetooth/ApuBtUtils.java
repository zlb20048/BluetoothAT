package com.service.bluetooth;

public final class ApuBtUtils
{
    static int convertBondToPairingState(int bondState)
    {
        switch (bondState)
        {
            case BluetoothDevice.BOND_BONDED:
                return BTConstants.BT_PAIRING_STATUS_SUCCEEDED;
            case BluetoothDevice.BOND_BONDING:
                return BTConstants.BT_PAIRING_STATUS_IN_PROCESS;
            default:
                return BTConstants.BT_PAIRING_STATUS_FAILED;
        }
    }

    static int convertPairingToBondState(int pairingState)
    {
        switch (pairingState)
        {
            case BTConstants.BT_PAIRING_STATUS_BAD_PIN:
                return BluetoothDevice.BOND_NONE;
            case BTConstants.BT_PAIRING_STATUS_FAILED:
                return BluetoothDevice.BOND_NONE;
            case BTConstants.BT_PAIRING_STATUS_IN_PROCESS:
                return BluetoothDevice.BOND_BONDING;
            case BTConstants.BT_PAIRING_STATUS_MAX_DEV_REACHED:
                return BluetoothDevice.BOND_NONE;
            case BTConstants.BT_PAIRING_STATUS_SUCCEEDED:
                return BluetoothDevice.BOND_BONDED;
            case BTConstants.BT_PAIRING_STATUS_NOT_FOUND:
                return BluetoothDevice.BOND_NONE;
            default:
                return BluetoothDevice.BOND_NONE;
        }
    }

    static int convertCallStateToDeviceCallState(int callState)
    {
        switch (callState)
        {
            case BTConstants.BT_CALL_STATE_FREE:
                return BluetoothDevice.CALL_STATE_IDLE;
            case BTConstants.BT_CALL_STATE_ACTIVE:
                return BluetoothDevice.CALL_STATE_OFFHOOK;
            case BTConstants.BT_CALL_STATE_DAILING:
                return BluetoothDevice.CALL_STATE_DIALING;
            case BTConstants.BT_CALL_STATE_HELD:
                return BluetoothDevice.CALL_STATE_OFFHOOK;
            case BTConstants.BT_CALL_STATE_WAITING:
                return BluetoothDevice.CALL_STATE_RINGING;
            case BTConstants.BT_CALL_STATE_RINGING:
                return BluetoothDevice.CALL_STATE_DIALING;
            case BTConstants.BT_CALL_STATE_RELEASED:
                return BluetoothDevice.CALL_STATE_IDLE;
            case BTConstants.BT_CALL_STATE_INCOMING:
                return BluetoothDevice.CALL_STATE_RINGING;
            default:
                return BluetoothDevice.CALL_STATE_IDLE;
        }
    }

    static int convertstateToConnectingState(int callState)
    {
        switch (callState)
        {
            case BTConstants.BT_DEVICE_STATE_CONNECT_OK:
                return BluetoothDevice.CONNECT_CONNECTED;
            case BTConstants.BT_DEVICE_STATE_CONNECT_FAILED:
                return BluetoothDevice.CONNECT_DISCONNECTED;
            case BTConstants.BT_DEVICE_STATE_CONNECT_NOT_FOUND:
                return BluetoothDevice.CONNECT_DISCONNECTED;
            case BTConstants.BT_DEVICE_STATE_DISCONNECT_FAILED:
                return BluetoothDevice.CONNECT_DISCONNECTED;
            case BTConstants.BT_DEVICE_STATE_AUTOCONNECT:
                return BluetoothDevice.CONNECT_DISCONNECTED;
            case BTConstants.BT_DEVICE_STATE_CONNECT_ABORT:
                return BluetoothDevice.CONNECT_DISCONNECTED;
            case BTConstants.BT_DEVICE_STATE_CONNECT_DEFAULT_SERVICE:
                return BluetoothDevice.CONNECT_CONNECTED;
            case BTConstants.BT_DEVICE_STATE_USER_ALLREADY_STARTING:
                return BluetoothDevice.CONNECT_CONNECTED;
            default:
                return BluetoothDevice.CONNECT_DISCONNECTED;
        }
    }

    static int convertPlayerStatus(int playerState)
    {
        switch (playerState)
        {
            case BTConstants.IVT_PLAY_STATUS_STOP:
                return BluetoothDevice.PLAYER_STATUS_STOP;
            case BTConstants.IVT_PLAY_STATUS_PAUSED:
                return BluetoothDevice.PLAYER_STATUS_PAUSE;
            case BTConstants.IVT_PLAY_STATUS_PLAYING:
            case BTConstants.IVT_PLAY_STATUS_FSTFWD:
            case BTConstants.IVT_PLAY_STATUS_REWIND:
                return BluetoothDevice.PLAYER_STATUS_PLAY;
            default:
                return BluetoothDevice.PLAYER_STATUS_STOP;
        }
    }

    static int convertPlayerStatusByA2DPState(int a2dpState)
    {
        switch (a2dpState)
        {
            case BTConstants.IVT_A2DP_STATE_STREAM:
                return BluetoothDevice.PLAYER_STATUS_PLAY;
            case BTConstants.IVT_A2DP_STATE_CONNTECTED:
                return BluetoothDevice.PLAYER_STATUS_PAUSE;
            default:
                return BluetoothDevice.PLAYER_STATUS_STOP;
        }
    }


    static int convertstateToRepeateMode(int repeateMode)
    {
        switch (repeateMode)
        {
            case BTConstants.PALYER_MODE_REPEATE_ALL:
                return BluetoothDevice.REPEATE_ALL_SONGS;
            case BTConstants.PALYER_MODE_REPEATE_CURRENT:
                return BluetoothDevice.REPEATE_CURRENT_SONG;
            default:
                return BluetoothDevice.REPEATE_CURRENT_SONG;
        }
    }
}
