package com.example.sairamkrishna.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity  {
    String deviceName2="";
    private static final String TAG = "LOGAN";
    Button b1,b2,b3,b4;
    private BluetoothAdapter BA;
    Handler h;

    final int RECIEVE_MESSAGE = 1;        // Status  for Handler
    public BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();

    public ConnectedThread mConnectedThread;
    private BluetoothDevice deviceName;
    String deviceHardwareAddress="";
    private Set<BluetoothDevice>pairedDevices;
    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        b1 = (Button) findViewById(R.id.button);
        b2=(Button)findViewById(R.id.button2);
        b3=(Button)findViewById(R.id.button3);
        b4=(Button)findViewById(R.id.button4);

        BA = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                deviceName2 = device.getName();
                Log.d(TAG, "Name - " + deviceName2);
                deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, "deviceHardwareAddress - " + deviceHardwareAddress);

            }
        }


        //00:06:66:A0:9F:96
        lv = (ListView)findViewById(R.id.listView);


    }

    public void on(View v){
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    public void off(View v){
        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }
    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
            Log.d(TAG, "SDK>15");
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }
    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(BA==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (BA.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }
    public  void visible(View v){
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }


    public void list(View v){
        Log.d(TAG, "...Connect Button Pressed...");



        BA = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        BluetoothDevice device = BA.getRemoteDevice(deviceHardwareAddress);
        Log.d(TAG, "...THEMAINHardwareAddress..."+deviceHardwareAddress);

        try {
            Log.d(TAG, "...CreateBTSocket...");
            btSocket = createBluetoothSocket(device);

        } catch (IOException e) {
            Log.d(TAG, "...FKED up...");
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        BA.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "...Connecting...");
                try {
                    Log.d(TAG, "....Enter try...");

                    btSocket.connect();
                    Log.d(TAG, "....Connection ok...");
                    // Create a data stream so we can talk to server.
                    Log.d(TAG, "...Create Socket...");

                    mConnectedThread = new ConnectedThread(btSocket);
                    mConnectedThread.start();
                } catch (IOException e) {
                    Log.d(TAG, "....Entering Catch...");
                    e.printStackTrace();
                    try {
                        Log.d(TAG, "....Closing Socket...");
                        btSocket.close();
                    } catch (IOException e2) {
                        Log.d(TAG, "....Fatal Error...");
                        errorExit("Fatal Error", "close socket during connection failure" + e2.getMessage() + ".");
                    }
                }
            }
        });
    }

    public class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, "..IOException : " + e.getMessage() + "...");
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer); // Get number of bytes and message in "buffer"

                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    Log.d(TAG, "..Exception : " + e.getMessage() + "...");
                    //Log.d(TAG, Log.getStackTraceString(new Exception()));
                    break;
                }
            }
        }


        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }
}