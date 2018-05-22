package com.devogelaere.seppe.bluetoothapp;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    String btName, btAddress;
    TextView tempTxt, humTxt;
    Button dataBtn, ledBtn;
    boolean ledState = false;
    final String TAG = "MainActivity";

    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    InputStream inputStream;
    int readBufferPosition;
    byte[] readBuffer;
    Thread workerThread;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btName = getIntent().getStringExtra(DevicesActivity.DEVICE_NAME);
        btAddress = getIntent().getStringExtra(DevicesActivity.DEVICE_ADDRESS);
        setTitle(btName);

        tempTxt = findViewById(R.id.tempTxt);
        humTxt = findViewById(R.id.humTxt);

        dataBtn = findViewById(R.id.dataBtn);
        ledBtn = findViewById(R.id.ledBtn);

        registerComponents();
        new ConnectBT().execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                disconnect();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void registerComponents() {
        dataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getTempHumidity();
            }
        });

        ledBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeLed();
            }
        });
    }

    private void setLedButton() {
        if (ledState) {
            ledBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red));
        } else {
            ledBtn.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.normalButton));
        }
    }

    public void setLedChecked(String s) {
        ledState = s.substring(0, 1).equals("1");
        setLedButton();
    }

    private void setTextfields(String temp, String hum) {
        tempTxt.setText(getString(R.string.temperature, temp));
        humTxt.setText(hum + "%");

    }

    private void getTempHumidity() {
        if (btSocket!=null) {
            try {
                inputStream = btSocket.getInputStream();
                btSocket.getOutputStream().write("temp".getBytes());

                dataListener();
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void changeLed() {
        if (btSocket!=null) {
            try {
                btSocket.getOutputStream().write("changeLed".getBytes());
                ledState = !ledState;
                setLedButton();
            }
            catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void disconnect() {
        if (btSocket!=null) {
            try {
                btSocket.close();
                msg("Disconnected");
            } catch (IOException e) {
                msg("Error");
            }
        }
        finish();
    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    void dataListener()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        isBtConnected = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !isBtConnected) {
                    try {
                        int bytesAvailable = inputStream.available();
                        if(bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            for(int i=0;i<bytesAvailable;i++) {
                                byte b = packetBytes[i];
                                if(b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            try {
                                                String[] separated = data.split(";");
                                                if (separated[0].equals("DHT11")) {
                                                    setTextfields(separated[1], separated[2]);
                                                    setLedChecked(separated[3]);
                                                }
                                            } catch (Exception e) {
                                                Log.e(TAG, e.getMessage());
                                            }
                                        }
                                    });
                                }
                                else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        isBtConnected = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>
    {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait...");
        }

        @Override
        protected Void doInBackground(Void... devices)
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice btDevice = myBluetooth.getRemoteDevice(btAddress);
                    btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is a Bluetooth device connected?");
                finish();
            }
            else
            {
                msg("Connected");
                isBtConnected = true;
                getTempHumidity();
            }
            progress.dismiss();
        }
    }

}
