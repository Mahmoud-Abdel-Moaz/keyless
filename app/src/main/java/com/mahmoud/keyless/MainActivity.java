package com.mahmoud.keyless;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG="MainActivity";

    TextView txt_stats;
    Switch lock_switch;
    Intent intent;
    String lang;
    StringBuilder message;
    BluetoothAdapter myBluetooth;
    Set<BluetoothDevice> piaredDevices;
    BluetoothSocket btSocket;
    String addrss,name;
    Thread thread;
    BluetoothDevice mBTDevice;

    Handler handler;
    private static UUID MY_UUID_INSECURE=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt_stats=findViewById(R.id.txt_stats);
        lock_switch=findViewById(R.id.lock_switch);

        try {
            myBluetooth=BluetoothAdapter.getDefaultAdapter();
            addrss=myBluetooth.getAddress();
            piaredDevices=myBluetooth.getBondedDevices();
            if (piaredDevices.size()>0){
                for (BluetoothDevice bt:piaredDevices)
                {
                    addrss=bt.getAddress().toString();
                    name=bt.getName().toString();

                    //   Toast.makeText(this, addrss+" "+name, Toast.LENGTH_SHORT).show();
                    if (addrss.equals("")||name.equals("HC-05")){
                        mBTDevice=bt;
                        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                        Method getUuidsMethod = BluetoothAdapter.class.getDeclaredMethod("getUuids", null);
                        ParcelUuid[] uuids = (ParcelUuid[]) getUuidsMethod.invoke(adapter, null);
                        mBTDevice.getUuids();
                        for (ParcelUuid uuid: uuids) {
                            //  Toast.makeText(this, uuid.getUuid().toString(), Toast.LENGTH_SHORT).show();
                            MY_UUID_INSECURE=uuid.getUuid();
                        }
                    }

                }
            }
        }catch (Exception we){
            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
        }

        handler=new Handler();

        startConnection();

        lock_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    txt_stats.setText("On");
                    try {
                        if (btSocket!=null){
                            btSocket.getOutputStream().write("on".getBytes());
                            // Toast.makeText(MiddleActivity.this, "Connect", Toast.LENGTH_SHORT).show();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    txt_stats.setText("Off");
                    try {
                        if (btSocket!=null){
                            btSocket.getOutputStream().write("off".getBytes());
                            // Toast.makeText(MiddleActivity.this, "Connect", Toast.LENGTH_SHORT).show();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void startConnection(){
        try {
            startBTConnection(mBTDevice,MY_UUID_INSECURE);
        }catch (Exception e){

        }
    }

    //starting service method
    public void startBTConnection(BluetoothDevice device,UUID uuid){
        Log.d(TAG,"startBTConnection: Initializing RFCOM Bluetooth Connection.");
        if(device!=null){
            myBluetooth=BluetoothAdapter.getDefaultAdapter();
            device=myBluetooth.getRemoteDevice(device.getAddress());
            try {
                if (!btSocket.isConnected()){
                    btSocket=device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));//create a RFCOMM (SPP) connection
                    btSocket.connect();
                }
                Connection connection=new Connection(btSocket);
                thread=new Thread(connection);
                thread.start();
            } catch (IOException e) {
                // Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                try {
                    btSocket.close();
                    if (!btSocket.isConnected()){
                        btSocket=device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));//create a RFCOMM (SPP) connection
                        btSocket.connect();
                        lock_switch.setEnabled(true);
                    }
                    Connection connection=new Connection(btSocket);
                    thread=new Thread(connection);
                    lock_switch.setEnabled(true);
                    thread.start();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

        }else {
        }

    }

    public class Connection extends Thread{

        BluetoothSocket socket;

        public Connection( BluetoothSocket socket){
            this.socket=socket;
            //   Toast.makeText(LevelsActivity.this, "hi", Toast.LENGTH_SHORT).show();
            //  Thread thread = new Thread(this);
            //  thread.start();
        }
        public void run(){

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while (true){
                try {
                    Log.d(TAG,"run start");
                    byte[] buffer=new byte[1024];// buffer store for the stream
                    int bytes;//bytes returned from read();
                    bytes=socket.getInputStream().read(buffer);
                    final String incomingMessage=new String(buffer,0,bytes);
                    // but_low.setText("hi");
                    Log.d(TAG,incomingMessage);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (incomingMessage.equals("off")){
                                txt_stats.setText("Off");
                                lock_switch.setChecked(false);
                            }else if (incomingMessage.equals("on")){
                                txt_stats.setText("On");
                                lock_switch.setChecked(true);
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
