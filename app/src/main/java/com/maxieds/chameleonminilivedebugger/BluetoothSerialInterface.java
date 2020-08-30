/*
This program (The Chameleon Mini Live Debugger) is free software written by
Maxie Dion Schmidt: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

The complete license provided with source distributions of this library is
available at the following link:
https://github.com/maxieds/ChameleonMiniLiveDebugger
*/

package com.maxieds.chameleonminilivedebugger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothConfiguration;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothService;
import com.github.douglasjunior.bluetoothclassiclibrary.BluetoothStatus;
import com.github.douglasjunior.bluetoothlowenergylibrary.BluetoothLeService;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class BluetoothSerialInterface extends SerialIOReceiver {

    private static final String TAG = BluetoothSerialInterface.class.getSimpleName();

    public static final String CHAMELEON_REVG_NAME = "BLE-Chameleon";
    public static final String CHAMELEON_REVG_SERVICE_UUID = "51510001-7969-6473-6f40-6b6f6c6c6957";
    public static final String CHAMELEON_REVG_SEND_CHAR_UUID = "51510002-7969-6473-6f40-6b6f6c6c6957";
    public static final String CHAMELEON_REVG_RECV_CHAR_UUID = "51510003-7969-6473-6f40-6b6f6c6c6957";
    public static final String CHAMELEON_REVG_TINY_NAME = "ChameleonTiny";
    public static final String CHAMELEON_REVG_TINY_SERVICE_UUID = "51510001-7969-6473-6f40-6b6f6c6c6957";
    public static final String CHAMELEON_REVG_TINY_SEND_CHAR_UUID = "51510002-7969-6473-6f40-6b6f6c6c6957";
    public static final String CHAMELEON_REVG_TINY_RECV_CHAR_UUID = "51510003-7969-6473-6f40-6b6f6c6c6957";

    public String getInterfaceLoggingTag() {
        return "SerialBTReader";
    }

    private Context notifyContext;
    private BluetoothService btServiceSend, btServiceRecv;
    private BluetoothConfiguration btConfigSend, btConfigRecv;
    private BluetoothDevice activeDevice;
    private int baudRate; // irrelevant?
    private boolean serialConfigured;
    private boolean receiversRegistered;
    private final Semaphore serialPortLock = new Semaphore(1, true);

    public boolean isBluetoothEnabled() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null) {
            return false;
        }
        return btAdapter.isEnabled();
    }

    public static void displayAndroidBluetoothSettings() {
        Intent intentOpenBluetoothSettings = new Intent();
        intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        LiveLoggerActivity.getInstance().startActivity(intentOpenBluetoothSettings);
    }

    public boolean configureSerialConnection(BluetoothDevice btDev) {
        if(btDev == null) {
            return false;
        }
        else if(btDev.getName() == null || (!btDev.getName().equals(CHAMELEON_REVG_NAME) && !btDev.getName().equals(CHAMELEON_REVG_TINY_NAME))) {
            return false;
        }
        if(!receiversRegistered) {
            configureSerial();
        }
        activeDevice = btDev;
        Log.i(TAG, "BTDEV: " + activeDevice.toString());
        try {
            if (btDev.getName().equals(CHAMELEON_REVG_NAME)) {
                btConfigSend.uuidService = UUID.fromString(CHAMELEON_REVG_SERVICE_UUID);
                btConfigSend.uuidCharacteristic = UUID.fromString(CHAMELEON_REVG_SEND_CHAR_UUID);
                btConfigRecv.uuidService = UUID.fromString(CHAMELEON_REVG_SERVICE_UUID);
                btConfigRecv.uuidCharacteristic = UUID.fromString(CHAMELEON_REVG_RECV_CHAR_UUID);
            } else {
                btConfigSend.uuidService = UUID.fromString(CHAMELEON_REVG_TINY_SERVICE_UUID);
                btConfigSend.uuidCharacteristic = UUID.fromString(CHAMELEON_REVG_TINY_SEND_CHAR_UUID);
                btConfigRecv.uuidService = UUID.fromString(CHAMELEON_REVG_TINY_SERVICE_UUID);
                btConfigRecv.uuidCharacteristic = UUID.fromString(CHAMELEON_REVG_TINY_RECV_CHAR_UUID);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        ChameleonIO.REVE_BOARD = false;
        btConfigSend.deviceName = btConfigRecv.deviceName = btDev.getName();
        btServiceSend = initBluetoothListeners(btConfigSend);
        btServiceRecv = initBluetoothListeners(btConfigRecv);
        btServiceSend.connect(btDev);
        btServiceRecv.connect(btDev);
        Settings.chameleonDeviceSerialNumber = btDev.getAddress();
        return true;
    }

    public String getDeviceName() {
        return activeDevice.getName();
    }

    public boolean isDevicePaired() {
        if(activeDevice == null) {
            return false;
        }
        return btServiceSend.getStatus() == BluetoothStatus.CONNECTED && btServiceRecv.getStatus() == BluetoothStatus.CONNECTED;
    }

    public BluetoothSerialInterface(Context appContext) {
        notifyContext = appContext;
        btConfigSend = initBluetoothConfig();
        btConfigRecv = initBluetoothConfig();
        btServiceSend = initBluetoothListeners(btConfigSend);
        btServiceRecv = initBluetoothListeners(btConfigRecv);
        activeDevice = null;
        baudRate = Settings.serialBaudRate;
        serialConfigured = false;
        receiversRegistered = false;
    }

    public BluetoothConfiguration initBluetoothConfig() {
        BluetoothConfiguration btConfig = new BluetoothConfiguration();
        btConfig.context = notifyContext;
        btConfig.bluetoothServiceClass = BluetoothLeService.class;
        btConfig.bufferSize = 256;
        btConfig.characterDelimiter = '\n';
        btConfig.deviceName = "ChameleonMiniLiveDebugger";
        btConfig.callListenersInMainThread = true;
        btConfig.uuidService = null;
        btConfig.uuidCharacteristic = null;
        btConfig.transport = BluetoothDevice.TRANSPORT_LE;
        btConfig.uuid = null; // Used to filter found devices. Set null to find all devices.
        BluetoothService.init(btConfig);
        return btConfig;
    }

    public BluetoothService initBluetoothListeners(BluetoothConfiguration btConfig) {
        BluetoothService.init(btConfig);
        BluetoothService btService = BluetoothService.getDefaultInstance();
        btService.disconnect();
        btService.setOnScanCallback(new BluetoothService.OnBluetoothScanCallback() {
            @Override
            public void onDeviceDiscovered(BluetoothDevice device, int rssi) {
                if(!serialConfigured && configureSerialConnection(device)) {
                    stopScanningDevices();
                    if (!Settings.serialIOPorts[Settings.USBIO_IFACE_INDEX].serialConfigured()) {
                        if (Settings.serialIOPorts[Settings.BTIO_IFACE_INDEX].configureSerial() != 0) {
                            ChameleonIO.PAUSED = false;
                            serialConfigured = true;
                            notifyStatus("BT STATUS: ", "Successfully configured the device \"" + activeDevice.getName() + "\" in passive logging mode...\n" + getActiveDeviceInfo());
                            notifyBluetoothChameleonDeviceConnected();
                        }
                    }
                }
            }
            @Override
            public void onStartScan() {}
            @Override
            public void onStopScan() {}
        });
        btService.setOnEventCallback(new BluetoothLeService.OnBluetoothEventCallback() {
            @Override
            public void onDataRead(byte[] buffer, int length) {
                Log.d(TAG, "BTREAD: " + Utils.bytes2Ascii(buffer));
                Log.d(TAG, "BTREAD: " + Utils.bytes2Hex(buffer));
                if(length > 0) {
                    onReceivedData(buffer);
                }
            }
            @Override
            public void onStatusChange(BluetoothStatus status) {}
            @Override
            public void onDeviceName(String deviceName) {
                Log.i(TAG, "New BT Device Name: " + deviceName);
            }
            @Override
            public void onToast(String message) {}
            @Override
            public void onDataWrite(byte[] buffer) {}
        });
        return btService;
    }

    public void setListenerContext(Context context) {
        notifyContext = context;
    }

    public boolean notifySerialDataReceived(byte[] serialData) {
        Log.d(TAG, "BTReaderCallback Serial Data: (HEX) " + Utils.bytes2Hex(serialData));
        Log.d(TAG, "BTReaderCallback Serial Data: (TXT) " + Utils.bytes2Ascii(serialData));
        Intent notifyIntent = new Intent(ChameleonSerialIOInterface.SERIALIO_DATA_RECEIVED);
        notifyIntent.putExtra("DATA", serialData);
        notifyContext.sendBroadcast(notifyIntent);
        return true;
    }

    public boolean notifyLogDataReceived(byte[] serialData) {
        Log.d(TAG, "BTReaderCallback Log Data: (HEX) " + Utils.bytes2Hex(serialData));
        Log.d(TAG, "BTReaderCallback Log Data: (TXT) " + Utils.bytes2Ascii(serialData));
        if(serialData.length < ChameleonLogUtils.LOGGING_MIN_DATA_BYTES + 4) {
            return false;
        }
        Intent notifyIntent = new Intent(ChameleonSerialIOInterface.SERIALIO_LOGDATA_RECEIVED);
        notifyIntent.putExtra("DATA", serialData);
        notifyContext.sendBroadcast(notifyIntent);
        return true;
    }

    public boolean notifyDeviceFound() {
        Log.i(TAG, "notifyDeviceFound");
        Intent notifyIntent = new Intent(ChameleonSerialIOInterface.SERIALIO_DEVICE_FOUND);
        notifyContext.sendBroadcast(notifyIntent);
        return true;
    }

    public boolean notifyDeviceConnectionTerminated() {
        Log.i(TAG, "notifyDeviceConnectionTerminated");
        Intent notifyIntent = new Intent(ChameleonSerialIOInterface.SERIALIO_DEVICE_CONNECTION_LOST);
        notifyContext.sendBroadcast(notifyIntent);
        return true;
    }

    public boolean notifyStatus(String msgType, String statusMsg) {
        Log.i(TAG, "notifyStatus: " + msgType + ": " + statusMsg);
        Intent notifyIntent = new Intent(ChameleonSerialIOInterface.SERIALIO_NOTIFY_STATUS);
        notifyIntent.putExtra("STATUS-TYPE", msgType);
        notifyIntent.putExtra("STATUS-MSG", statusMsg);
        notifyContext.sendBroadcast(notifyIntent);
        return true;
    }

    public boolean notifyBluetoothChameleonDeviceConnected() {
        Log.i(TAG, "notifyBluetoothChameleonDeviceConnected");
        Intent notifyIntent = new Intent(ChameleonSerialIOInterface.SERIALIO_NOTIFY_BTDEV_CONNECTED);
        notifyContext.sendBroadcast(notifyIntent);
        return true;
    }

    public boolean isWiredUSB() { return false; }

    public boolean isBluetooth() { return true; }

    public int setSerialBaudRate(int brate) {
        baudRate = brate;
        Settings.serialBaudRate = baudRate;
        return baudRate;
    }
    public int setSerialBaudRateHigh() {
        return setSerialBaudRate(ChameleonSerialIOInterface.HIGH_SPEED_BAUD_RATE);
    }
    public int setSerialBaudRateLimited() {
        return setSerialBaudRate(ChameleonSerialIOInterface.LIMITED_SPEED_BAUD_RATE);
    }

    public boolean startScanningDevices() {
        configureSerial();
        btServiceSend.startScan();
        return true;
    }

    public boolean stopScanningDevices() {
        btServiceSend.stopScan();
        return true;
    }

    public String getActiveDeviceInfo() {
        if(activeDevice == null) {
            return "<null-device>: No information available.";
        }
        String devInfo = String.format(Locale.ENGLISH, "BT Class: %s\nBond State: %s\nProduct Name: %s\nType: %s\nDevice Address: %s",
                activeDevice.getBluetoothClass(), activeDevice.getBondState(),
                activeDevice.getName(), activeDevice.getType(),
                activeDevice.getAddress());;
        return devInfo;
    }

    public int configureSerial() {
        if(serialConfigured()) {
            return STATUS_TRUE;
        }
        receiversRegistered = true;
        return STATUS_TRUE;
    }

    public int shutdownSerial() {
        if(btServiceSend != null) {
            btServiceSend.stopScan();
            btServiceSend.disconnect();
            btServiceSend.stopService();
        }
        if(btServiceRecv != null) {
            btServiceRecv.stopScan();
            btServiceRecv.disconnect();
            btServiceRecv.stopService();
        }
        ChameleonIO.PAUSED = true;
        ExportTools.EOT = true;
        ExportTools.transmissionErrorOccurred = true;
        ChameleonIO.DOWNLOAD = false;
        ChameleonIO.UPLOAD = false;
        ChameleonIO.WAITING_FOR_XMODEM = false;
        ChameleonIO.WAITING_FOR_RESPONSE = false;
        ChameleonIO.EXPECTING_BINARY_DATA = false;
        ChameleonIO.LASTCMD = "";
        ChameleonIO.APPEND_PRIOR_BUFFER_DATA = false;
        ChameleonIO.PRIOR_BUFFER_DATA = new byte[0];
        activeDevice = null;
        serialConfigured = false;
        receiversRegistered = false;
        notifyDeviceConnectionTerminated();
        return STATUS_TRUE;
    }

    public boolean serialConfigured() { return serialConfigured; }

    public boolean serialReceiversRegistered() { return receiversRegistered; }

    public boolean acquireSerialPort() {
        try {
            serialPortLock.acquire();
            return true;
        } catch(Exception inte) {
            inte.printStackTrace();
            serialPortLock.release();
            return false;
        }
    }

    public boolean acquireSerialPortNoInterrupt() {
        try {
            serialPortLock.acquireUninterruptibly();
            return true;
        } catch(Exception inte) {
            inte.printStackTrace();
            serialPortLock.release();
            return false;
        }
    }

    public boolean tryAcquireSerialPort(int timeout) {
        boolean status = false;
        try {
            status = serialPortLock.tryAcquire(timeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            return status;
        } catch(Exception inte) {
            inte.printStackTrace();
            serialPortLock.release();
            return false;
        }
    }

    public boolean releaseSerialPortLock() {
        serialPortLock.release();
        return true;
    }

    public int sendDataBuffer(byte[] dataWriteBuffer) {
        if(dataWriteBuffer == null || dataWriteBuffer.length == 0) {
            return STATUS_FALSE;
        }
        else if(!serialConfigured()) {
            return STATUS_FALSE;
        }
        btServiceSend.write(dataWriteBuffer);
        return STATUS_TRUE;
    }

}
