package com.devogelaere.seppe.bluetoothapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class DevicesActivity extends AppCompatActivity {

    ListView devicelist;
    SwipeRefreshLayout swipeRefresh;
    final String TAG = "DevicesActivity";
    private static final int ENABLE_BLUETOOTH_REQUEST = 1;

    private BluetoothAdapter btAdapter = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String DEVICE_NAME = "device_name";
    public static String DEVICE_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        swipeRefresh = findViewById(R.id.swiperefresh);
        devicelist = findViewById(R.id.listView);

        registerComponents();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ENABLE_BLUETOOTH_REQUEST:
                getDevices();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void registerComponents() {
        checkBTAdapter();

        swipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        getDevices();
                    }
                }
        );
    }

    private void checkBTAdapter() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(btAdapter == null)
        {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available on this device.", Toast.LENGTH_LONG).show();
            finish();
        }
        else if(btAdapter.isEnabled()) {
            getDevices();
        }
        else if(!btAdapter.isEnabled())
        {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),ENABLE_BLUETOOTH_REQUEST);
        }
    }

    private void getDevices()
    {
        pairedDevices = btAdapter.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size()>0)
        {
            for(BluetoothDevice bt : pairedDevices)
            {
                list.add(bt.getName() + "\n" + bt.getAddress());
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {
                String info = ((TextView) view).getText().toString();
                String[] split = info.split("\n");

                Intent i = new Intent(DevicesActivity.this, MainActivity.class);

                i.putExtra(DEVICE_NAME, split[0]);
                i.putExtra(DEVICE_ADDRESS, split[1]);
                startActivity(i);
            }
        });
        swipeRefresh.setRefreshing(false);
    }
}
