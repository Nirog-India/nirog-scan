package com.apps.hrgiri.nirogscan;

import android.bluetooth.BluetoothGattCharacteristic;

public interface BtControllerCallback {
    void onBtDisconnected();
    void onGattReady();
    void onCharacteristicReadSuccess(BluetoothGattCharacteristic characteristic);
    void onCharacteristicWriteSuccess(BluetoothGattCharacteristic characteristic);
    void onCharacteristicNotification(BluetoothGattCharacteristic characteristic);
}
