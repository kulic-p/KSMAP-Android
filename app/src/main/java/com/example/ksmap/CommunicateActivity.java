package com.example.ksmap;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

public class CommunicateActivity extends AppCompatActivity {

    private TextView connectionText, textViewTemp, textViewHum, textViewTempReal, textViewHumLimit;
    private Button connectButton;
    private ToggleButton toggleButtonOnOff;
    private SeekBar seekBar;

    private CommunicateViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicate);

        // Zobrazeni tlacitka zpet na hornim panelu
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // nastaveni viewModel
        viewModel = ViewModelProviders.of(this).get(CommunicateViewModel.class);

        // Metoda vrati false, pokud nastane error - zavreni komunikace
        if (!viewModel.setupViewModel(getIntent().getStringExtra("device_name"), getIntent().getStringExtra("device_mac"))) {
            finish();
            return;
        }

        connectionText = findViewById(R.id.communicate_connection_text);
        connectButton = findViewById(R.id.communicate_connect);
        toggleButtonOnOff = findViewById(R.id.toggleButtonOnOff);
        textViewTemp = findViewById(R.id.textViewTemp);
        textViewHum = findViewById(R.id.textViewHum);
        textViewTempReal = findViewById(R.id.textViewTempReal);
        seekBar = findViewById(R.id.seekBar);
        textViewHumLimit = findViewById(R.id.textViewHumLimit);

        // Sledovani dat, ktere zasle ViewModel
        viewModel.getConnectionStatus().observe(this, this::onConnectionStatus);
        viewModel.getDeviceName().observe(this, name -> setTitle(getString(R.string.device_name_format, name)));

        //nastaveni pole teploty
        viewModel.getTemp().observe(this, temp -> {
            textViewTemp.setText(temp);
        });

        //nastaveni pole pocitove teploty
        viewModel.getRealTemp().observe(this, tmp -> {
            textViewTempReal.setText(tmp);
        });

        //nastaveni pole vlhkosti
        viewModel.getHum().observe(this, hum -> {
            textViewHum.setText(hum);
        });

        //nastaveni pole limitu vlhkosti
        viewModel.getHumLimit().observe(this, hum -> {
            textViewHumLimit.setText(hum);
        });

        toggleButtonOnOff.setOnClickListener(v -> {
            if (toggleButtonOnOff.isChecked()) {
                viewModel.sendMessage("ON");
            } else {
                viewModel.sendMessage("OFF");
            }

        });

        //posunuti slideru
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int tmp = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                viewModel.sendMessage("L" + progress * 10);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    // Pokud se ve ViewModel zmeni status, je volano nasledujici
    private void onConnectionStatus(CommunicateViewModel.ConnectionStatus connectionStatus) {
        switch (connectionStatus) {
            case CONNECTED:
                connectionText.setText(R.string.status_connected);
                toggleButtonOnOff.setEnabled(true);
                connectButton.setEnabled(true);
                seekBar.setEnabled(true);
                connectButton.setText(R.string.disconnect);
                connectButton.setOnClickListener(v -> viewModel.disconnect());
                break;

            case CONNECTING:
                connectionText.setText(R.string.status_connecting);
                toggleButtonOnOff.setEnabled(false);
                connectButton.setEnabled(false);
                seekBar.setEnabled(false);
                connectButton.setText(R.string.connect);
                break;

            case DISCONNECTED:
                connectionText.setText(R.string.status_disconnected);
                toggleButtonOnOff.setEnabled(false);
                connectButton.setEnabled(true);
                seekBar.setEnabled(false);
                connectButton.setText(R.string.connect);
                connectButton.setOnClickListener(v -> viewModel.connect());
                break;
        }
    }

    // Pri stisknuti tlacitka zpet v hornim panelu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {

        // Ukonceni aktivity
        finish();
    }
}
