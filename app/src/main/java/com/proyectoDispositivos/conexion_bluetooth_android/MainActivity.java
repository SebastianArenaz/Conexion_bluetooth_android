package com.proyectoDispositivos.conexion_bluetooth_android;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setControsl();
    }

    SeekBar seekBar;//barra a la que se le realiza el movimiento
    TextView seekProgress,textoaumento;
    MediaPlayer mp;
int contador;
    protected void setControsl() {
        contador=0;
        seekBar = findViewById(R.id.seekBar);
        seekProgress = findViewById(R.id.progress_seek);
        mp = MediaPlayer.create(this, R.raw.sonido_agua);

        textoaumento= findViewById(R.id.aumentoagua);
        textoaumento.setTextSize(20);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progreso, boolean b) {
                seekProgress.setText(progreso + "/100");

                if(contador<progreso){
                    contador=progreso;
                    mp.start();
                    textoaumento.setTextSize(progreso+20);
                }else{
                    textoaumento.setTextSize(progreso+20);
                    contador=progreso;

                }
                if(progreso>=100){
                    textoaumento.setTextSize(progreso+70);
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
}