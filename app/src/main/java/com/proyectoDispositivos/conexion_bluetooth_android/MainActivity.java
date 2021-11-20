package com.proyectoDispositivos.conexion_bluetooth_android;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.proyectoDispositivos.conexion_bluetooth_android.helper.EnhancedSharedPreferences;
import com.proyectoDispositivos.conexion_bluetooth_android.helper.NotificationHelper;
import com.proyectoDispositivos.conexion_bluetooth_android.service.MyBluetoothSerialService;
import com.proyectoDispositivos.conexion_bluetooth_android.util.Config;
import com.proyectoDispositivos.conexion_bluetooth_android.util.Constants;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Set;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String EXTRA_DEVICE_ADDRESS = "device_address";
    private static final int REQUEST_ENABLE_BT = 1;

    private MainActivity.MyServiceMessageHandler myServiceMessageHandler;
    protected MyBluetoothSerialService myBluetoothSerialService = null;
    private String mConnectedDeviceName = null;
    private boolean mBoundService = false;
    private EnhancedSharedPreferences sharedPref;

    private BluetoothAdapter adaptador;
    private Button btnListarDispositivos, btnActivarBluetooth,btnEnviarDatos;
    private SeekBar seekBar;//barra a la que se le realiza el movimiento
    private TextView seekProgress, textoaumento;
    private MediaPlayer mp;
    int contador,Nivelprogreso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NotificationHelper notificationHelper = new NotificationHelper(this);
        notificationHelper.createChannels();

        // check support
        adaptador = BluetoothAdapter.getDefaultAdapter();
        if (adaptador == null) {
            Config.Mensaje(this, getString(R.string.text_no_bluetooth_adapter), false, false);
        } else {
            Intent intent = new Intent(getApplicationContext(), MyBluetoothSerialService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        /**
         * Creamos el hilo para responder a los siguientes estados
         * MESSAGE_STATE_CHANGE
         * MESSAGE_DEVICE_NAME
         * MESSAGE_TOAST
         */
        myServiceMessageHandler = new MainActivity.MyServiceMessageHandler(this, this);
        setControsl();
        adaptador = BluetoothAdapter.getDefaultAdapter();
        sharedPref = EnhancedSharedPreferences.getInstance(getApplicationContext(), getString(R.string.shared_preference_key));

    }

    protected void setControsl() {
        contador = 0;
        seekBar = findViewById(R.id.seekBar);
        seekProgress = findViewById(R.id.progress_seek);
        mp = MediaPlayer.create(this, R.raw.sonido_agua);
        textoaumento = findViewById(R.id.aumentoagua);
        btnActivarBluetooth = findViewById(R.id.btnActivarBluetooth);
        btnListarDispositivos = findViewById(R.id.btnListarDispositivos);
        btnEnviarDatos=findViewById(R.id.btnEnviarDatos);

        adaptador = BluetoothAdapter.getDefaultAdapter();

        textoaumento.setTextSize(20);

        btnListarDispositivos.setOnClickListener(this);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progreso, boolean b) {
                seekProgress.setText(progreso + "/100");
                Nivelprogreso=progreso;
                if (contador < progreso) {
                    contador = progreso;
                    mp.start();
                    textoaumento.setTextSize(progreso + 20);

                } else {
                    textoaumento.setTextSize(progreso + 20);
                    contador = progreso;

                }
                if (progreso >= 100) {
                    textoaumento.setTextSize(progreso + 70);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            //Esto se llama cuando la conexión con el servicio ha sido
            // establecido, dándonos el objeto de servicio que podemos usar para
            // interactuar con el servicio
            MyBluetoothSerialService.MySerialServiceBinder binder = (MyBluetoothSerialService.MySerialServiceBinder) service;
            myBluetoothSerialService = binder.getService(); //Obtenermos la vinculacion del servicio
            mBoundService = true; //Variable para saber que el servicio esta conectado
            myBluetoothSerialService.setMessageHandler(myServiceMessageHandler); //Seteamos el hilo principal
            myBluetoothSerialService.setStatusUpdatePoolInterval(
                    Long.parseLong(sharedPref.getString(getString(
                            R.string.preference_update_pool_interval),
                            String.valueOf(Constants.STATUS_UPDATE_INTERVAL)))); // indicamos con cuanta frecuencia va a realiza actualizaciones el servicio
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Esto se llama cuando la conexión con el servicio ha sido
            // desconectado inesperadamente, es decir, su proceso se bloqueó.
            mBoundService = false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.CONNECT_DEVICE_INSECURE:
            case Constants.CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {

                    mConnectedDeviceName = Objects.requireNonNull(data.getExtras()).getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Log.e("MI_DATO", "" + mConnectedDeviceName);
                    connectToDevice(mConnectedDeviceName);
                }
        }
    }

    private static class MyServiceMessageHandler extends Handler {

        private final WeakReference<MainActivity> mActivity;
        private final Context mContext;

        MyServiceMessageHandler(Context context, MainActivity activity) {
            mContext = context;
            mActivity = new WeakReference<>(activity);
        }

        //Recibimos los datos enviados
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    mActivity.get().onBluetoothStateChange(msg.arg1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    mActivity.get().mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Config.Mensaje(mContext, mActivity.get().getString(R.string.text_connected_to) + " " + mActivity.get().mConnectedDeviceName, false, false);
                    break;
                case Constants.MESSAGE_TOAST:
                    Config.Mensaje(mContext, msg.getData().getString(Constants.TOAST), false, false);
                    break;
            }
        }
    }

    private void onBluetoothStateChange(int currentState) {

        switch (currentState) {
            case MyBluetoothSerialService.STATE_CONNECTED:
                //Esta conectado
                break;
            case MyBluetoothSerialService.STATE_CONNECTING:
                //Esta conectandose
                break;
            case MyBluetoothSerialService.STATE_LISTEN:
                //Recibiendo datos
                break;
            case MyBluetoothSerialService.STATE_NONE:
                //Indicar que no esta conectado el bluetooth
                break;
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        //Activamos el bluetooth por default
        if (!adaptador.isEnabled()) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        adaptador.enable(); //Activamos el BT
                    } catch (RuntimeException e) {
                        Toast.makeText(MainActivity.this, "No se puede iniciar bluetooth, revise sus permisos", Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            };
            thread.start();
        }

        //Preguntamos por el estado actual del servicio
        if (myBluetoothSerialService != null)
            onBluetoothStateChange(myBluetoothSerialService.getState());
    }

    @Override
    public void onClick(View view) {

        //Conectarse a bluetooth
        if (view.getId() == R.id.btnActivarBluetooth) {
            setEstadoBluetooth();
            if (adaptador.isEnabled()) {
                if (myBluetoothSerialService != null) {
                    if (myBluetoothSerialService.getState() == myBluetoothSerialService.STATE_CONNECTED) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.text_disconnect)
                                .setMessage(getString(R.string.text_disconnect_confirm))
                                .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (myBluetoothSerialService != null)
                                            myBluetoothSerialService.disconnectService();
                                    }
                                })
                                .setNegativeButton(getString(R.string.text_cancel), null)
                                .show();

                    } else {
                        Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(serverIntent, Constants.CONNECT_DEVICE_SECURE);
                    }
                } else {
                    Toast.makeText(this, "Servicio Bluetooth no esta ejecutandose!", Toast.LENGTH_SHORT).show();
                }

            } else {
                Config.Mensaje(MainActivity.this, getString(R.string.text_bt_not_enabled), false, false);
            }
        }


        if (view.getId() == R.id.btnListarDispositivos) {

            Intent i = new Intent(MainActivity.this, DeviceListActivity.class);
            startActivityForResult(i, Constants.CONNECT_DEVICE_SECURE);

            /*new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.text_disconnect)
                    .setMessage(R.string.text_disconnect_confirm)
                    .setPositiveButton(getString(R.string.text_yes_confirm),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int winch) {
                                    Toast.makeText(MainActivity.this, "Boton conectar", Toast.LENGTH_LONG).show();

                                }
                            })
                    .setNegativeButton(getString(R.string.text_cancel), null)
                    .show();*/
        }
        /*Toast.makeText(this, listar_dispositivos_vinculados(),
                Toast.LENGTH_LONG).show();*/

        if (view.getId() == R.id.btnEnviarDatos) {

            if(myBluetoothSerialService != null && myBluetoothSerialService.getState() == MyBluetoothSerialService.STATE_CONNECTED){
                myBluetoothSerialService.serialWriteString(String.valueOf(Nivelprogreso)); // Enviamos la data a nuestro arduino
            }else {
                Toast.makeText(this, "No se logró enviar el dato", Toast.LENGTH_SHORT).show();
            }

        }

    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };

    private void connectToDevice(String macAddress) {
        if (macAddress == null) {
            //Si el nombre es nulo entonces volvemos a mostrar la lista de dispositivos para que se vuelva a conectar
            Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
            startActivityForResult(serverIntent, Constants.CONNECT_DEVICE_SECURE);
        } else {
            ;
            Intent intent = new Intent(getApplicationContext(), MyBluetoothSerialService.class);
            intent.putExtra(MyBluetoothSerialService.KEY_MAC_ADDRESS, macAddress);

            //Verificamos que sea la version 26(Oreo) a superior esto se debe
            // a las limitaciones por consumo de bateria que realizo los desarrolladores de google
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
                getApplicationContext().startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBoundService) {
            myBluetoothSerialService.setMessageHandler(null);
            unbindService(serviceConnection);
            mBoundService = false;
        }

        stopService(new Intent(this, MyBluetoothSerialService.class));
    }


    /*
        private String listar_dispositivos_vinculados() {
            String cadena = "";
            Set<BluetoothDevice> pairedDevices = adaptador.getBondedDevices();
            if (pairedDevices.size() > 0) {
                // listamos los dispositivos Bluetooth vinculados en el celular
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    cadena += deviceName;
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    cadena += " - " + deviceHardwareAddress + "\n";
                }

            }
            return cadena;
        }
    */
    public void setEstadoBluetooth() {

        if (!adaptador.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(this, "Bluetooth ya está activado", Toast.LENGTH_LONG).show();
        }

    }
}