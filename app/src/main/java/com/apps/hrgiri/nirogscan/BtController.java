package com.apps.hrgiri.nirogscan;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.apps.hrgiri.nirogscan.Constants.BATTERY_CHARACTERISTIC_UUID;
import static com.apps.hrgiri.nirogscan.Constants.FNP_CHARACTERISTIC_UUID;
import static com.apps.hrgiri.nirogscan.Constants.READ_CHARACTERISTIC_UUID;
import static com.apps.hrgiri.nirogscan.Constants.SERVICE_UUID;

public class BtController implements Serializable{
    private static BtController btController = null;
    public static final String EXTRA_MESSAGE = "com.apps.hrgiri.nirogscan.DEVICE_ADDRESS";
    public static final int ENABLE_BLUETOOTH_REQUEST_CODE = 1;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 2;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothDevice esp;
    public BluetoothGatt btGatt;

    private Context context;
    private Activity activity;

    private BtControllerCallback callback;

    private boolean isScanning = false;
    private boolean foundDevice = false;

    private BtController(Context context, BluetoothManager btManager, Activity activity) {
        this.context = context;
        this.activity = activity;
        bluetoothAdapter = btManager.getAdapter();

        if (context instanceof BtControllerCallback) {
            callback = (BtControllerCallback) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement BtControllerCallback");
        }
    }

    public void setCallback(Context context){
        if (context instanceof BtControllerCallback) {
            callback = (BtControllerCallback) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement BtControllerCallback");
        }
    }
    public static BtController getInstance(){
        return btController;
    }
    public static BtController setInstance(Context context, BluetoothManager btManager, Activity activity){
        btController = new BtController(context,btManager,activity);
        return btController;
    }

    // To be called from Activity's onResume
    public void onResume() {
        if (!bluetoothAdapter.isEnabled()) {
            promptEnableBluetooth();
        }
    }
    // To be called from Activity's onActivityResult
    public void onActivityResult(int requestCode, int resultCode,Intent data) {
        switch (requestCode) {
            case ENABLE_BLUETOOTH_REQUEST_CODE:
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth();
                }
        }
    }
    // To be called from Activity's onRequestPermissionsResult
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startBleScan();
                } else {
                    requestLocationPermission();
                }
                break;
        }
    }

    public void promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE);
        }
    }

    private boolean isLocationPermissionGranted(){
        return (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }
    public void startBleScan(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted()) {
            requestLocationPermission();
        }
        else {
            ScanFilter nameFilter = new ScanFilter.Builder()
                    .setDeviceName("BOOTH_GATT_SERVER")
                    .build();
            List<ScanFilter> filters = new ArrayList<>();
            filters.add(nameFilter);
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            bleScanner.startScan(filters,scanSettings,scanCallback);
            setScanning(true);
        }
    }

    public void stopBleScan() {
        bleScanner.stopScan(scanCallback);
        setScanning(false);
    }
    private void requestLocationPermission(){
        if (isLocationPermissionGranted()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Starting from Android M (6.0), the system requires apps to be granted " +
                "location access in order to scan for BLE devices.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(
                                activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                    }
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        alert.show();
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(!foundDevice) {
                foundDevice = true;
                Log.i("ScanCallback", "Found BLE device! Name: " + result.getDevice().getName() + ", address: " + result.getDevice().getAddress());
                if (isScanning) {
                    setScanning(false);
                    stopBleScan();
                }
                esp = result.getDevice();
                Log.w("ScanCallback", "Connecting to " + esp.getAddress());
                esp.connectGatt(context, true, gattCallback,BluetoothDevice.TRANSPORT_LE);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("ScanCallback","Error Code: " + errorCode);
        }
    };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String deviceAddress = gatt.getDevice().getAddress();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to " + deviceAddress);
                    btGatt = gatt;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            btGatt.discoverServices();
                        }
                    });
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from " + deviceAddress);
                    gatt.close();
                    callback.onBtDisconnected();
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            infoMessageBox.setText("Not connected to sensing device");
//                            startSeshButton.setEnabled(true);
//                        }
//                    });
                }
            } else {
                Log.w("BluetoothGattCallback", "Error " + status + " encountered for " + deviceAddress + "! Disconnecting...");
                foundDevice = false;
                callback.onBtDisconnected();
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.w("BluetoothGattCallback", "Discovered " + gatt.getServices().size() + " services for " + esp.getAddress());
            gatt.requestMtu(500);
//            printGattTable(gatt);// See implementation just above this section
            // Consider connection setup as complete here
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.e("BluetoothGattCallback", "Status : " + status);
            printGattTable(gatt);// See implementation just above this section
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    Log.i("BluetoothGattCallback", "Read characteristic " + characteristic.getUuid() + ":\t" + characteristic.getStringValue(0));
                    callback.onCharacteristicReadSuccess(characteristic);
                    break;
                case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                    Log.e("BluetoothGattCallback", "Read not permitted for " + characteristic.getUuid() + "!");
                    break;
                default:
                    Log.e("BluetoothGattCallback", "Characteristic read failed for " + characteristic.getUuid() + ", error: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    Log.i("BluetoothGattCallback", "Write characteristic " + characteristic.getUuid() + ":\t" + characteristic.getStringValue(0));
                    callback.onCharacteristicWriteSuccess(characteristic);
                    BluetoothGattCharacteristic btChar;
                    switch (characteristic.getUuid().toString()){
                        case BATTERY_CHARACTERISTIC_UUID:
                            btChar = gatt.getService(UUID.fromString(SERVICE_UUID)).getCharacteristic(UUID.fromString(FNP_CHARACTERISTIC_UUID));
                            gatt.setCharacteristicNotification(btChar,true);
                            byte[] writeValue = {1};
                            btChar.setValue(writeValue);
                            gatt.writeCharacteristic(btChar);
                            break;
                        case  FNP_CHARACTERISTIC_UUID:
                            btChar = gatt.getService(UUID.fromString(SERVICE_UUID)).getCharacteristic(UUID.fromString(READ_CHARACTERISTIC_UUID));
                            gatt.setCharacteristicNotification(btChar,true);
                            callback.onGattReady();
                            break;
                    }
                    break;
                case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                    Log.e("BluetoothGattCallback", "Write not permitted for " + characteristic.getUuid() + "!");
                    break;
                default:
                    Log.e("BluetoothGattCallback", "Characteristic read failed for " + characteristic.getUuid() + ", error: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i("BluetoothGattCallback", "Notify characteristic " + characteristic.getUuid() + ":\t" + characteristic.getStringValue(0));
            callback.onCharacteristicNotification(characteristic);
//            btGatt.readCharacteristic(characteristic);
        }


        private void printGattTable(BluetoothGatt gatt) {
            if (gatt.getServices().isEmpty()) {
                Log.i("printGattTable", "No service and characteristic available, call discoverServices() first?");
                return;
            }

            for (BluetoothGattService service : gatt.getServices()) {
                String characteristicsTable = "|--";
                StringBuilder stringBuilder = new StringBuilder();
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()){
                    stringBuilder.append(characteristic.getUuid());
                    stringBuilder.append("\t" + characteristic.getStringValue(0));
                    stringBuilder.append("\n\t|--");
                    for (String property : getPropetiesString(characteristic)){
                        stringBuilder.append(property);
                        stringBuilder.append("\n\t|--");
                    }
                    stringBuilder.append("\n|--");
                }
                characteristicsTable += stringBuilder;
                Log.i("printGattTable", "\nService " + service.getUuid() + "\nCharacteristics:\n" + characteristicsTable);

//                Intent intent = new Intent(MainActivity.this, Main2Activity.class);
//                intent.putExtra(EXTRA_MESSAGE, gatt.getDevice().getAddress());
//                startActivity(intent);
//                gatt.readCharacteristic(
//                        gatt.getService(UUID.fromString("0000aa-0000-1000-8000-00805f9b34fb"))
//                        .getCharacteristic(UUID.fromString("0000aa01-0000-1000-8000-00805f9b34fb")));
                BluetoothGattCharacteristic characteristic = btGatt.getService(UUID.fromString(SERVICE_UUID)).getCharacteristic(UUID.fromString(BATTERY_CHARACTERISTIC_UUID));
                btGatt.setCharacteristicNotification(characteristic,true);
                byte[] writeValue = {1};
                characteristic.setValue(writeValue);
                btGatt.writeCharacteristic(characteristic);
//                callback.onGattReady();
            }
        }

        private boolean isReadable(BluetoothGattCharacteristic characteristic){
            return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0;
        }

        private boolean isWritable(BluetoothGattCharacteristic characteristic){
            return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0;
        }

        private boolean isNotifiable(BluetoothGattCharacteristic characteristic){
            return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
        }

        private List<String> getPropetiesString(BluetoothGattCharacteristic characteristic){
            List<String> properties = new ArrayList<>();
            if(isReadable(characteristic)) properties.add("Read");
            if(isWritable(characteristic)) properties.add("Write");
            if(isNotifiable(characteristic)) properties.add("Notify");
            if( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_BROADCAST) != 0 ) properties.add("Broadcast");
            if( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS) != 0 ) properties.add("Extended Props");
            if( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0 ) properties.add("Indicate");
            if( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE) != 0 ) properties.add("Signed Write");
            if( (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 ) properties.add("Write No Response");

            return properties;
        }

    };

    private void setScanning(final boolean value){
        isScanning = value;
    }
    public boolean isScanning(){
        return isScanning;
    }
}
