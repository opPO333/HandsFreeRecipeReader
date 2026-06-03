package app.recipe.reader.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import app.recipe.reader.R;
import app.recipe.reader.data.AppDatabase;
import app.recipe.reader.data.Recipe;
import app.recipe.reader.data.RecipeJsonParser;
import app.recipe.reader.data.RecipeStep;

public class BluetoothShareActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    private static final UUID APP_UUID = UUID.fromString("20895b5e-1061-49f5-be05-81a31dd9b67a");
    private static final String APP_NAME = "RecipeReaderBT";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int REQUEST_DISCOVERABLE = 3;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> devicesAdapter;
    private List<BluetoothDevice> discoveredDevices = new ArrayList<>();

    private TextView tvStatus;
    private ProgressBar progressBar;
    private ListView listDevices;

    private int recipeIdToSend = -1;
    private boolean isSendingMode = false;
    private AppDatabase db;

    private AcceptThread acceptThread;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        androidx.activity.EdgeToEdge.enable(this);
        
        setContentView(R.layout.activity_bluetooth_share);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bt_main), (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvStatus = findViewById(R.id.tv_bluetooth_status);
        progressBar = findViewById(R.id.progress_bar);
        listDevices = findViewById(R.id.list_devices);

        db = AppDatabase.getInstance(this);

        recipeIdToSend = getIntent().getIntExtra(EXTRA_RECIPE_ID, -1);
        isSendingMode = recipeIdToSend != -1;

        devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        listDevices.setAdapter(devicesAdapter);

        listDevices.setOnItemClickListener((parent, view, position, id) -> {
            if (isSendingMode) {
                BluetoothDevice device = discoveredDevices.get(position);
                connectToDevice(device);
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        checkPermissionsAndStart();
    }

    private void checkPermissionsAndStart() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            startBluetoothLogic();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startBluetoothLogic();
            } else {
                Toast.makeText(this, "Permissions required for Bluetooth!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startBluetoothLogic() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            setupBluetooth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                setupBluetooth();
            } else {
                Toast.makeText(this, "Bluetooth must be enabled!", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == REQUEST_DISCOVERABLE) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Device must be discoverable to receive recipes!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void setupBluetooth() {
        if (isSendingMode) {
            tvStatus.setText("Scanning for nearby devices...");
            startDiscovery();
        } else {
            tvStatus.setText("Waiting to receive a recipe...");
            progressBar.setVisibility(View.VISIBLE);

            listDevices.setVisibility(View.GONE);
            TextView label = findViewById(R.id.tv_available_devices_label);
            label.setVisibility(View.GONE);

            acceptThread = new AcceptThread();
            acceptThread.start();

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE);
        }
    }

    @SuppressLint("MissingPermission")
    String getDeviceName(BluetoothDevice device) {
        return device.getName() == null ? device.getAddress() : device.getName();
    }

    @SuppressLint("MissingPermission")
    boolean checkDevice(BluetoothDevice device) {
        return device != null && device.getBluetoothClass() != null && device.getBluetoothClass().getMajorDeviceClass() == android.bluetooth.BluetoothClass.Device.Major.PHONE;
    }

    @SuppressLint("MissingPermission")
    private void startDiscovery() {
        devicesAdapter.clear();
        discoveredDevices.clear();
        progressBar.setVisibility(View.VISIBLE);

        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (checkDevice(device)) {
                discoveredDevices.add(device);
                devicesAdapter.add(getDeviceName(device));
            }
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device != null && !discoveredDevices.contains(device) && checkDevice(device)) {
                    discoveredDevices.add(device);
                    devicesAdapter.add(getDeviceName(device));
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        bluetoothAdapter.cancelDiscovery();
        tvStatus.setText("Connecting to " + getDeviceName(device) + "...");
        progressBar.setVisibility(View.VISIBLE);
        
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    private void onConnected(BluetoothSocket socket) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            if (isSendingMode) {
                tvStatus.setText("Connected! Sending recipe...");
                connectedThread = new ConnectedThread(socket);
                connectedThread.start();
                sendRecipeData();
            } else {
                tvStatus.setText("Connected! Receiving recipe...");
                connectedThread = new ConnectedThread(socket);
                connectedThread.start();
            }
        });
    }

    private void sendRecipeData() {
        AppDatabase.databaseExecutor.execute(() -> {
            try {
                Recipe recipe = db.recipeDao().getAllRecipes().stream().filter(r -> r.getId() == recipeIdToSend).findFirst().orElse(null);

                if (recipe != null) {
                    List<RecipeStep> steps = db.recipeDao().getStepsForRecipe(recipeIdToSend);
                    String json = RecipeJsonParser.serialize(recipe, steps);
                    
                    if (connectedThread != null) {
                        connectedThread.write(json.getBytes());
                        
                        runOnUiThread(() -> {
                            tvStatus.setText("Recipe sent successfully!");
                            Toast.makeText(BluetoothShareActivity.this, "Sent!", Toast.LENGTH_SHORT).show();
                            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1000);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvStatus.setText("Error serializing recipe!"));
            }
        });
    }

    private void onRecipeReceived(String json) {
        runOnUiThread(() -> tvStatus.setText("Recipe received! Saving..."));
        
        AppDatabase.databaseExecutor.execute(() -> {
            try {
                RecipeJsonParser.ParsedRecipe parsed = RecipeJsonParser.deserialize(json);
                long newRecipeId = db.recipeDao().insertRecipe(parsed.recipe);
                
                for (RecipeStep step : parsed.steps) {
                    step.setRecipeId((int) newRecipeId);
                }
                db.recipeDao().insertSteps(parsed.steps);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Recipe saved!", Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvStatus.setText("Error parsing received recipe!"));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) { }

        if (acceptThread != null) acceptThread.cancel();
        if (connectThread != null) connectThread.cancel();
        if (connectedThread != null) connectedThread.cancel();
    }

    @SuppressLint("MissingPermission")
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) { }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                if (socket != null) {
                    onConnected(socket);
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) { }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                if (mmServerSocket != null) mmServerSocket.close();
            } catch (IOException e) { }
        }
    }

    @SuppressLint("MissingPermission")
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                runOnUiThread(() -> {
                    Toast.makeText(BluetoothShareActivity.this, "Connection failed!", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("Scanning for nearby devices...");
                    progressBar.setVisibility(View.VISIBLE);
                    bluetoothAdapter.startDiscovery();
                });
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            onConnected(mmSocket);
        }

        public void cancel() {
            try {
                if (mmSocket != null) mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            StringBuilder data = new StringBuilder();

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    String chunk = new String(buffer, 0, bytes);
                    data.append(chunk);

                    if (mmInStream.available() == 0) {
                        Thread.sleep(100);

                        if (mmInStream.available() == 0 && data.length() > 0) {
                            String finalData = data.toString();
                            if (finalData.startsWith("{") && finalData.endsWith("}")) {
                                onRecipeReceived(finalData);
                                data.setLength(0);
                            }
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
