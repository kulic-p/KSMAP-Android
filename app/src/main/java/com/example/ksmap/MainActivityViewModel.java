package com.example.ksmap;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.harrysoft.androidbluetoothserial.BluetoothManager;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class MainActivityViewModel extends AndroidViewModel {

    private BluetoothManager bluetoothManager;

    // Seznam sparovanych zarizeni, ktere vidi aktivita
    private MutableLiveData<Collection<BluetoothDevice>> pairedDeviceList = new MutableLiveData<>();

    // Promenna, ktera nam pomaha nenastavovat nic dvakrat
    private boolean viewModelSetup = false;

    // Toto je pouze konstruktor, který je volán systémem, a odpovídá AndroidViewModel
    public MainActivityViewModel(@NotNull Application application) {
        super(application);
    }

    //Vola se v onCreate(). Zkontroluje, zda již byl volan, a pokud ne, nastavi data.
    // Vrati hodnotu true, pokud vse probehlo v poradku, nebo false, pokud doslo k chybe
    public boolean setupViewModel() {
        // Overeni zda uz nebyl volan
        if (!viewModelSetup) {
            viewModelSetup = true;

            // Nastaveni BluetoothManager
            bluetoothManager = BluetoothManager.getInstance();
            if (bluetoothManager == null) {
                // Bluetooth neni k dispozici
                Toast.makeText(getApplication(), R.string.no_bluetooth, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    // Obnova listu sparovanych zarizeni
    public void refreshPairedDevices() {
        pairedDeviceList.postValue(bluetoothManager.getPairedDevicesList());
    }

    // Zavolana pokud je aktivita dokoncena
    @Override
    protected void onCleared() {
        if (bluetoothManager != null)
            bluetoothManager.close();
    }

    // Getter ktery vyuziva aktivita
    public LiveData<Collection<BluetoothDevice>> getPairedDeviceList() {
        return pairedDeviceList;
    }
}
