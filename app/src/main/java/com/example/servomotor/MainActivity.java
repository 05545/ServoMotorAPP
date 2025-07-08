package com.example.servomotor;

/*
HECHO POR RODRIGO SOSA ROMERO
GITHUB: https://github.com/05545
 */

/*
Este archivo es la activity principal que maneja la interfaz y operaciones de envio y recepcion mediante
el uso de la clase bluetooth manager, esencialmente es la que es intermediaria entre la vista y lo
enviado y recibido a través de los sockets bluetooth y el device correspondiente a la esp32
 */

/*
Importante hacer mención que no se hace uso de listas desplegables por lo que es importante que el
nombre del dispositivo vinculado esté correctamente definido o, en su defecto, hacer las modificaciones
necesarias para la parte de la lista desplegable y su funcionamiento completo para una correcta conexión. En caso de
querer hace dicha mejora los archivos a modificar son:

1. MainActivity
2. BluetoothManager
3. activity_main
 */

/*
Es importante recordar que la implementación de este Main, sus valores y los datos que envia y recibe, además de
procesarlos, deben estar correctamente relacionados con el código cargado en la ESP32, de lo contrario aunque
la app y la conexión sean exitosos, puede carecer de funcionalidad en razón de la diferencia de los datos enviados
y/o recibidos. En caso de querer que la parte funcional quede de forma similar entonces se puede aplicar el código
esp32 disponible en este mismo repositorio. Haciendo mención de que se hace con una esp32 38 pinout.
 */

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements BluetoothManager.BluetoothListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    // UI Components
    private Button btnConnect;
    private TextView tvConnectionStatus;
    private TextView tvCurrentAngle;
    private View statusIndicator;
    private SeekBar seekBarAngle;
    private TachometerView tachometerView;
    private Button btn0, btn45, btn90, btn135, btn180;

    // Bluetooth
    private BluetoothManager bluetoothManager;
    private boolean isUpdatingFromESP32 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupListeners();

        // Inicializar Bluetooth Manager
        bluetoothManager = new BluetoothManager(this, this);

        // Verificar permisos y configurar Bluetooth
        initializeBluetooth();
    }

    private void initializeViews() {
        btnConnect = findViewById(R.id.btnConnect);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvCurrentAngle = findViewById(R.id.tvCurrentAngle);
        statusIndicator = findViewById(R.id.statusIndicator);
        seekBarAngle = findViewById(R.id.seekBarAngle);
        tachometerView = findViewById(R.id.tachometerView);

        btn0 = findViewById(R.id.btn0);
        btn45 = findViewById(R.id.btn45);
        btn90 = findViewById(R.id.btn90);
        btn135 = findViewById(R.id.btn135);
        btn180 = findViewById(R.id.btn180);
    }

    private void setupListeners() {
        btnConnect.setOnClickListener(v -> toggleBluetoothConnection());

        // SeekBar listener
        seekBarAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && !isUpdatingFromESP32) {
                    updateAngle(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Botones de ángulos predefinidos
        btn0.setOnClickListener(v -> setPresetAngle(0));
        btn45.setOnClickListener(v -> setPresetAngle(45));
        btn90.setOnClickListener(v -> setPresetAngle(90));
        btn135.setOnClickListener(v -> setPresetAngle(135));
        btn180.setOnClickListener(v -> setPresetAngle(180));
    }

    private void initializeBluetooth() {
        if (!bluetoothManager.isBluetoothAvailable()) {
            showToast("Este dispositivo no tiene Bluetooth");
            btnConnect.setEnabled(false);
            return;
        }

        // Verificar permisos usando el método del BluetoothManager
        if (!bluetoothManager.hasBluetoothPermissions()) {
            bluetoothManager.requestBluetoothPermissions();
        } else {
            checkBluetoothEnabled();
        }
    }

    private void checkBluetoothEnabled() {
        if (!bluetoothManager.isBluetoothEnabled()) {
            requestEnableBluetooth();
        }
    }

    private void requestEnableBluetooth() {
        try {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } catch (SecurityException e) {
            // En caso de que no tengamos permisos, solicitarlos
            showToast("Se requieren permisos de Bluetooth");
            bluetoothManager.requestBluetoothPermissions();
        }
    }

    private void toggleBluetoothConnection() {
        // Verificar permisos antes de intentar conectar
        if (!bluetoothManager.hasBluetoothPermissions()) {
            bluetoothManager.requestBluetoothPermissions();
            return;
        }

        if (bluetoothManager.isConnected()) {
            bluetoothManager.disconnect();
        } else {
            if (!bluetoothManager.isBluetoothEnabled()) {
                showToast("Bluetooth no está habilitado");
                requestEnableBluetooth();
                return;
            }

            btnConnect.setEnabled(false);
            btnConnect.setText("Conectando...");

            if (!bluetoothManager.findAndConnectToESP32()) {
                // Si falla inmediatamente, habilitar el botón de nuevo
                btnConnect.setEnabled(true);
                btnConnect.setText("Conectar Bluetooth");
            }
        }
    }

    private void setPresetAngle(int angle) {
        updateAngle(angle);
        // Animar el seekbar
        seekBarAngle.setProgress(angle);
    }

    private void updateAngle(int angle) {
        // Actualizar UI
        tvCurrentAngle.setText(angle + "°");
        tachometerView.setAngle(angle);

        // Enviar comando al ESP32
        if (bluetoothManager.isConnected()) {
            bluetoothManager.sendAngle(angle);
        }
    }

    // Implementación de BluetoothListener
    @Override
    public void onConnectionChanged(boolean connected) {
        runOnUiThread(() -> {
            if (connected) {
                btnConnect.setText("Desconectar");
                btnConnect.setBackground(ContextCompat.getDrawable(this, R.drawable.button_preset));
                tvConnectionStatus.setText("Conectado");
                tvConnectionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                statusIndicator.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_green));
                showToast("Conectado al ESP32");
            } else {
                btnConnect.setText("Conectar Bluetooth");
                btnConnect.setBackground(ContextCompat.getDrawable(this, R.drawable.button_bluetooth));
                tvConnectionStatus.setText("Desconectado");
                tvConnectionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                statusIndicator.setBackground(ContextCompat.getDrawable(this, R.drawable.circle_red));
            }
            btnConnect.setEnabled(true);
        });
    }

    @Override
    public void onDataReceived(String data) {
        runOnUiThread(() -> {
            // Procesar datos recibidos del ESP32
            if (data.startsWith("POSITION:")) {
                try {
                    int angle = Integer.parseInt(data.substring(9));
                    isUpdatingFromESP32 = true;

                    // Actualizar UI con la posición del potenciómetro
                    tvCurrentAngle.setText(angle + "°");
                    tachometerView.setAngle(angle);
                    seekBarAngle.setProgress(angle);

                    isUpdatingFromESP32 = false;
                } catch (NumberFormatException e) {
                    // Ignorar datos mal formateados
                }
            }
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            showToast("Error: " + error);
            // Si hay error de conexión, resetear UI
            if (error.contains("Conexión perdida") || error.contains("Error al conectar")) {
                btnConnect.setEnabled(true);
                btnConnect.setText("Conectar Bluetooth");
            }
        });
    }

    @Override
    public void onPermissionRequired() {
        runOnUiThread(() -> {
            showToast("Se requieren permisos de Bluetooth");
            bluetoothManager.requestBluetoothPermissions();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                showToast("Bluetooth habilitado");
                // Una vez habilitado, verificar permisos de nuevo
                if (bluetoothManager.hasBluetoothPermissions()) {
                    // Bluetooth listo para usar
                } else {
                    bluetoothManager.requestBluetoothPermissions();
                }
            } else {
                showToast("Bluetooth es necesario para la aplicación");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS ||
                requestCode == BluetoothManager.REQUEST_BLUETOOTH_PERMISSIONS) {

            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                showToast("Permisos otorgados");
                checkBluetoothEnabled();

                // Notificar al BluetoothManager sobre los permisos
                if (bluetoothManager != null) {
                    bluetoothManager.onPermissionsResult(requestCode, permissions, grantResults);
                }
            } else {
                showToast("Permisos de Bluetooth son necesarios para el funcionamiento de la aplicación");
                btnConnect.setEnabled(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothManager != null) {
            bluetoothManager.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verificar estado de Bluetooth al volver a la actividad
        if (bluetoothManager != null && bluetoothManager.hasBluetoothPermissions()) {
            if (!bluetoothManager.isBluetoothEnabled()) {
                tvConnectionStatus.setText("Bluetooth deshabilitado");
                tvConnectionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}