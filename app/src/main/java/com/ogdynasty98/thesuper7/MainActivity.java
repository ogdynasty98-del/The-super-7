package com.ogdynasty98.thesuper7;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String DEVICE_ADDRESS = "E8:EF:16:21:70:E3";
    private static final int CONNECT_PERMISSION_REQUEST = 10;

    private TextView status;
    private Button connectButton;
    private BluetoothAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        connectButton = findViewById(R.id.connect_button);
        adapter = BluetoothAdapter.getDefaultAdapter();
        connectButton.setOnClickListener(view -> connect());
    }

    private void connect() {
        if (adapter == null) {
            show("This phone does not support Bluetooth.");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    CONNECT_PERMISSION_REQUEST);
            return;
        }
        if (!adapter.isEnabled()) {
            show("Turn on Bluetooth, then tap Connect again.");
            return;
        }

        BluetoothDevice device;
        try {
            device = adapter.getRemoteDevice(DEVICE_ADDRESS);
        } catch (IllegalArgumentException exception) {
            show("The configured Bluetooth address is invalid.");
            return;
        }

        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            show("Pair with Smokin’ Buds 360 in Android Bluetooth settings first.");
            return;
        }

        connectButton.setEnabled(false);
        status.setText(R.string.connecting);
        adapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                boolean requested = false;
                if (profile == BluetoothProfile.A2DP || profile == BluetoothProfile.HEADSET) {
                    try {
                        requested = (Boolean) proxy.getClass()
                                .getMethod("connect", BluetoothDevice.class)
                                .invoke(proxy, device);
                    } catch (ReflectiveOperationException ignored) {
                        // Android does not expose profile connection as a public API.
                    }
                }
                adapter.closeProfileProxy(profile, proxy);
                connectButton.setEnabled(true);
                show(requested
                        ? "Connection requested."
                        : "Android could not request a connection. Try pairing again.");
            }

            @Override
            public void onServiceDisconnected(int profile) {
                connectButton.setEnabled(true);
            }
        }, BluetoothProfile.A2DP);
    }

    private void show(String message) {
        status.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CONNECT_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connect();
        } else if (requestCode == CONNECT_PERMISSION_REQUEST) {
            show("Bluetooth permission is required to connect.");
        }
    }
}
