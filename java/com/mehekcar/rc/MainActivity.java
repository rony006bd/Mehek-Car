package com.mehekcar.rc;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_BT = 200;
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private OutputStream out;
    private TextView status;
    private Spinner devices;
    private SeekBar speed;
    private TextView speedLabel;
    private final ArrayList<BluetoothDevice> paired = new ArrayList<>();
    private LinearLayout rootLayout;
    private final ArrayList<Button> themedButtons = new ArrayList<>();
    private String selectedTheme = "Tarar Bari Green";
    private int accent = Color.rgb(0, 210, 115);
    private int surface = Color.rgb(12, 19, 36);
    private int buttonColor = Color.rgb(32, 112, 190);

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        adapter = BluetoothAdapter.getDefaultAdapter();
        buildUi();
        if (adapter == null) {
            status.setText("This phone does not support Bluetooth.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BT);
        } else {
            loadPairedDevices();
        }
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        rootLayout = root;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(12, 19, 36));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("MEHEK CAR", 30, Color.WHITE, true);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView subtitle = text("BLUETOOTH RC CONTROLLER", 13, Color.rgb(90, 205, 255), true);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(30)));

        status = text("Status: Not connected", 14, Color.rgb(255, 210, 90), true);
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(38)));

        devices = new Spinner(this);
        devices.setBackgroundColor(Color.WHITE);
        root.addView(devices, new LinearLayout.LayoutParams(-1, dp(48)));

        Spinner themePicker = new Spinner(this);
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Tarar Bari Green", "Ocean Blue", "Royal Purple", "Light", "Sunset Orange", "Red Alert"});
        themePicker.setAdapter(themeAdapter);
        root.addView(text("APP THEME", 13, Color.LTGRAY, true), new LinearLayout.LayoutParams(-1, dp(26)));
        root.addView(themePicker, new LinearLayout.LayoutParams(-1, dp(44)));
        themePicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int position, long id) {
                String[] themes = {"Tarar Bari Green", "Ocean Blue", "Royal Purple", "Light", "Sunset Orange", "Red Alert"};
                selectedTheme = themes[position];
                applyTheme(position);
            }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        Button connect = button("CONNECT TO PAIRED HC-05 / HC-06", Color.rgb(0, 150, 136));
        connect.setOnClickListener(v -> connectSelected());
        root.addView(connect, fullWidth());

        TextView speedTitle = text("SPEED", 16, Color.WHITE, true);
        root.addView(speedTitle, new LinearLayout.LayoutParams(-1, dp(32)));
        speedLabel = text("Speed level: 8", 14, Color.WHITE, false);
        root.addView(speedLabel, new LinearLayout.LayoutParams(-1, dp(26)));
        speed = new SeekBar(this);
        speed.setMax(9); speed.setProgress(8);
        root.addView(speed, fullWidth());
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean user) {
                speedLabel.setText("Speed level: " + p);
                if (user) send(String.valueOf(p));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });

        root.addView(text("DRIVE", 17, Color.WHITE, true), new LinearLayout.LayoutParams(-1, dp(42)));
        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER);
        row1.addView(new View(this), new LinearLayout.LayoutParams(dp(88), dp(68)));
        row1.addView(directionButton("▲", "F", "Forward"), buttonParams());
        row1.addView(new View(this), new LinearLayout.LayoutParams(dp(88), dp(68)));
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER);
        row2.addView(directionButton("◀", "L", "Left"), buttonParams());
        Button stop = directionButton("■", "S", "Stop");
        stop.setBackgroundColor(Color.rgb(220, 60, 70));
        row2.addView(stop, buttonParams());
        row2.addView(directionButton("▶", "R", "Right"), buttonParams());
        root.addView(row2);

        LinearLayout row3 = new LinearLayout(this);
        row3.setGravity(Gravity.CENTER);
        row3.addView(new View(this), new LinearLayout.LayoutParams(dp(88), dp(68)));
        row3.addView(directionButton("▼", "B", "Backward"), buttonParams());
        row3.addView(new View(this), new LinearLayout.LayoutParams(dp(88), dp(68)));
        root.addView(row3);

        root.addView(text("DIAGONAL CONTROL", 15, Color.WHITE, true), new LinearLayout.LayoutParams(-1, dp(42)));
        LinearLayout diag1 = new LinearLayout(this); diag1.setGravity(Gravity.CENTER);
        diag1.addView(directionButton("↖", "G", "Forward-left"), buttonParams());
        diag1.addView(directionButton("↗", "I", "Forward-right"), buttonParams());
        root.addView(diag1);
        LinearLayout diag2 = new LinearLayout(this); diag2.setGravity(Gravity.CENTER);
        diag2.addView(directionButton("↙", "H", "Backward-left"), buttonParams());
        diag2.addView(directionButton("↘", "J", "Backward-right"), buttonParams());
        root.addView(diag2);

        Button max = button("MAX SPEED (Q)", Color.rgb(85, 95, 210));
        max.setOnClickListener(v -> send("q"));
        root.addView(max, fullWidth());

        Button disconnect = button("DISCONNECT", Color.rgb(80, 90, 110));
        disconnect.setOnClickListener(v -> disconnect());
        root.addView(disconnect, fullWidth());

        TextView note = text("Pair your HC-05 in Android Bluetooth settings first. Typical pairing PIN: 1234 or 0000. Press STOP before disconnecting.", 12, Color.LTGRAY, false);
        root.addView(note, new LinearLayout.LayoutParams(-1, dp(68)));
    }

    private LinearLayout.LayoutParams fullWidth() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
        p.setMargins(0, dp(5), 0, dp(7)); return p;
    }
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(64), 1);
        p.setMargins(dp(4), dp(4), dp(4), dp(4)); return p;
    }
    private Button button(String label, int color) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE);
        b.setTextSize(14); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        themedButtons.add(b);
        return b;
    }
    private Button directionButton(String label, String command, String description) {
        Button b = button(label + "\n" + description, Color.rgb(32, 112, 190));
        b.setTextSize(13);
        b.setOnClickListener(v -> send(command));
        return b;
    }

    private void applyTheme(int index) {
        int textColor;
        switch (index) {
            case 1: surface = Color.rgb(5, 24, 48); accent = Color.rgb(0, 150, 255); buttonColor = Color.rgb(0, 105, 210); textColor = Color.WHITE; break;
            case 2: surface = Color.rgb(24, 12, 42); accent = Color.rgb(190, 90, 255); buttonColor = Color.rgb(105, 55, 175); textColor = Color.WHITE; break;
            case 3: surface = Color.rgb(245, 248, 252); accent = Color.rgb(0, 105, 220); buttonColor = Color.rgb(20, 105, 220); textColor = Color.rgb(20, 30, 45); break;
            case 4: surface = Color.rgb(38, 19, 5); accent = Color.rgb(255, 145, 25); buttonColor = Color.rgb(190, 85, 10); textColor = Color.WHITE; break;
            case 5: surface = Color.rgb(35, 8, 12); accent = Color.rgb(255, 55, 65); buttonColor = Color.rgb(190, 25, 40); textColor = Color.WHITE; break;
            default: surface = Color.rgb(12, 19, 36); accent = Color.rgb(0, 210, 115); buttonColor = Color.rgb(32, 112, 190); textColor = Color.WHITE; break;
        }
        if (rootLayout != null) rootLayout.setBackgroundColor(surface);
        for (Button b : themedButtons) {
            String label = b.getText().toString();
            int color = label.startsWith("■") ? Color.rgb(220, 35, 45) : buttonColor;
            if (label.startsWith("CONNECT")) color = accent;
            if (label.startsWith("MAX SPEED")) color = accent;
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        }
        if (status != null) status.setTextColor(accent);
        if (speedLabel != null) speedLabel.setTextColor(textColor);
    }

    private void loadPairedDevices() {
        try {
            Set<BluetoothDevice> set = adapter.getBondedDevices();
            paired.clear();
            ArrayList<String> names = new ArrayList<>();
            if (set != null) for (BluetoothDevice d : set) {
                paired.add(d);
                String name = d.getName();
                names.add((name == null ? "Bluetooth device" : name) + "  (" + d.getAddress() + ")");
            }
            if (names.isEmpty()) names.add("No paired devices — pair HC-05 in Settings");
            devices.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        } catch (SecurityException e) {
            status.setText("Allow Nearby devices permission and reopen the app.");
        }
    }

    private void connectSelected() {
        if (adapter == null) return;
        if (Build.VERSION.SDK_INT >= 31 &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BT);
            return;
        }
        int idx = devices.getSelectedItemPosition();
        if (idx < 0 || idx >= paired.size()) {
            status.setText("Pair HC-05 first in Android Bluetooth settings.");
            return;
        }
        status.setText("Connecting...");
        new Thread(() -> {
            try {
                BluetoothDevice d = paired.get(idx);
                BluetoothSocket s = d.createRfcommSocketToServiceRecord(SPP_UUID);
                adapter.cancelDiscovery();
                s.connect();
                OutputStream stream = s.getOutputStream();
                runOnUiThread(() -> {
                    socket = s; out = stream;
                    status.setText("Connected: " + (d.getName() == null ? "Bluetooth device" : d.getName()));
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Connection failed. Check HC-05 pairing/power."));
            }
        }).start();
    }

    private void send(String cmd) {
        try {
            if (out == null) {
                status.setText("Not connected — connect to HC-05 first.");
                return;
            }
            out.write(cmd.getBytes("US-ASCII"));
            out.flush();
        } catch (Exception e) {
            status.setText("Send failed. Reconnect Bluetooth.");
        }
    }

    private void disconnect() {
        send("S");
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null; out = null;
        status.setText("Status: Disconnected");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_BT) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) loadPairedDevices();
            else status.setText("Bluetooth permission is required to connect.");
        }
    }

    @Override public void onDestroy() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        super.onDestroy();
    }
}
