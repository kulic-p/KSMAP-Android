package com.example.ksmap;

import android.app.Application;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class CommunicateViewModel extends AndroidViewModel {

    // Promenna, ktera sleduje vsechny asynchronni tasky
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    // BluetoothManager
    private BluetoothManager bluetoothManager;

    // Bluetooth zarizeni. Pokud je odpojeno, je hodnota null
    @Nullable
    private SimpleBluetoothDeviceInterface deviceInterface;

    // message feed, ktery aktivita vidi
    private MutableLiveData<String> messagesData = new MutableLiveData<>();
    // Stav pripojeni, ktery aktivita vidi
    private MutableLiveData<ConnectionStatus> connectionStatusData = new MutableLiveData<>();
    // Jmeno yariyeni, ktere vyuziva aktivita
    private MutableLiveData<String> deviceNameData = new MutableLiveData<>();
    // Zprava v message boxu, ktere aktivita vidi
    private MutableLiveData<String> messageData = new MutableLiveData<>();

    // Teplota, kterou vidi aktivita
    private MutableLiveData<String> tempData = new MutableLiveData<>();

    // Realna teplota, kterou vidi aktivita
    private MutableLiveData<String> tempRealData = new MutableLiveData<>();

    // Vlhkost vzduchu, kterou vidi aktivita
    private MutableLiveData<String> humData = new MutableLiveData<>();

    // Vlhkost vzduchu, kterou vidi aktivita
    private MutableLiveData<String> humLimitData = new MutableLiveData<>();

    private StringBuilder messages = new StringBuilder();

    // Konfigurace zarizeni
    private String deviceName;
    private String mac;

    // Promenna aby jsme se nepripojili dvakrat
    private boolean connectionAttemptedOrMade = false;
    // Promenna aby jsme nenastovali zarizeni dvakrat
    private boolean viewModelSetup = false;

    // Called by the system, this is just a constructor that matches AndroidViewModel.
    public CommunicateViewModel(@NotNull Application application) {
        super(application);
    }

    // Je volana pri onCreate(). Kontroluje jestli byla nastavena jiz drive. Pokud ano, neprobehne
    // Vraci true pokud je vse ok, jinak false
    public boolean setupViewModel(String deviceName, String mac) {
        // Pojistka, aby neprobehlo dvakrat
        if (!viewModelSetup) {
            viewModelSetup = true;

            // Nastaveni bluetooth managera
            bluetoothManager = BluetoothManager.getInstance();

            if (bluetoothManager == null) {

                // Pokud telefon nema bluetooth
                toast(R.string.bluetooth_unavailable);

                // Navratova hodnota false do aktivity - nastala chyba
                return false;
            }

            // Nastaveni konfiguracnich promennych
            this.deviceName = deviceName;
            this.mac = mac;

            // Poslani jmena zarizeni do aktivity kvuli nastaveni titulku
            deviceNameData.postValue(deviceName);

            // Odpojeni zarizeni - poslani stavu do aktivity
            connectionStatusData.postValue(ConnectionStatus.DISCONNECTED);
        }
        return true;
    }

    // Akce vyvolana stisknutim tlacitka Pripojit
    public void connect() {

        // Overeni, zda se nepripojujeme, nebo nejsme pripojeni
        if (!connectionAttemptedOrMade) {

            // Asynchronni pripojeni
            compositeDisposable.add(bluetoothManager.openSerialDevice(mac)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(device -> onConnected(device.toSimpleDeviceInterface()), t -> {
                        toast(R.string.connection_failed);
                        connectionAttemptedOrMade = false;
                        connectionStatusData.postValue(ConnectionStatus.DISCONNECTED);
                    }));

            // Pripojeni probehlo uspesne
            connectionAttemptedOrMade = true;

            // Sdeleni aktivite, ze se pripojujeme
            connectionStatusData.postValue(ConnectionStatus.CONNECTING);
        }
    }

    // Zavolani pokud uzivatel zmackne tlacitko zpet
    public void disconnect() {
        // Kontrola, zda jsme pripojeni
        if (connectionAttemptedOrMade && deviceInterface != null) {
            connectionAttemptedOrMade = false;

            // Pouyiti knihovny a uzavreni spojeni
            bluetoothManager.closeDevice(deviceInterface);

            // nastaveni na null, aby ji nikdo nemohl pouzit
            deviceInterface = null;

            // Odeslani stavu, ze jsme odpojeni do aktivity
            connectionStatusData.postValue(ConnectionStatus.DISCONNECTED);
        }
    }

    // Zavolana, pokud se knihovna pripoji k BT zarizeni
    private void onConnected(SimpleBluetoothDeviceInterface deviceInterface) {
        this.deviceInterface = deviceInterface;
        if (this.deviceInterface != null) {

            // Pripojeni zarizeni, zmena statusu
            connectionStatusData.postValue(ConnectionStatus.CONNECTED);

            this.deviceInterface.setListeners(this::onMessageReceived, this::onMessageSent, t -> toast(R.string.message_send_error));

            // Toast, ze jsme pripojeni
            toast(R.string.connected);

            // Vymazani zprav
            messages = new StringBuilder();
            messagesData.postValue(messages.toString());
        } else {
            // Pripojeni selhalo - promenna byla null
            toast(R.string.connection_failed);
            connectionStatusData.postValue(ConnectionStatus.DISCONNECTED);
        }
    }

    // Konverze prijatych zprav z Arduina
    private void onMessageReceived(String message) {
        if (message.startsWith("T")) {

            tempData.postValue(message.substring(1) + "°C");
        } else if (message.startsWith("H")) {

            humData.postValue(message.substring(1) + "%");
        } else if (message.startsWith("R")) {

            tempRealData.postValue(message.substring(1) + "°C");
        } else if (message.startsWith("L")) {

            humLimitData.postValue(message.substring(1) + "%");
        } else
            messages.append(deviceName).append(": ").append(message).append('\n');
        messagesData.postValue(messages.toString());
    }

    // Pridani zpravy do konverzace
    private void onMessageSent(String message) {
        messages.append(getApplication().getString(R.string.you_sent)).append(": ").append(message).append('\n');
        messagesData.postValue(messages.toString());
        messageData.postValue("");
    }

    // Odeslani zpravy
    public void sendMessage(String message) {
        // Overeni, zda mame pripojene zarizeni a zda zprava neni prazdna
        if (deviceInterface != null && !TextUtils.isEmpty(message)) {
            deviceInterface.sendMessage(message);
        }
    }

    // Je zavolana po dokonceni aktivity
    @Override
    protected void onCleared() {
        compositeDisposable.dispose();
        // Uzavreni komunikace BT
        bluetoothManager.close();
    }

    // Pomocna metoda, ktera slouzi k vytvoreni toast notifikace
    private void toast(@StringRes int messageResource) {
        Toast.makeText(getApplication(), messageResource, Toast.LENGTH_LONG).show();
    }

    //Getter, ktery pouziva aktivita
    public LiveData<String> getMessages() {
        return messagesData;
    }

    //Getter, ktery pouziva aktivita
    public LiveData<ConnectionStatus> getConnectionStatus() {
        return connectionStatusData;
    }

    //Getter, ktery pouziva aktivita
    public LiveData<String> getDeviceName() {
        return deviceNameData;
    }

    //Getter, ktery pouziva aktivita
    public LiveData<String> getMessage() {
        return messageData;
    }

    //Getter, ktery pouziva aktivita
    public LiveData<String> getTemp() {
        return tempData;
    }

    //Getter, ktery pouziva aktivita
    public LiveData<String> getHum() {
        return humData;
    }

    //Getter, ktery pouziva aktivita
    public LiveData<String> getHumLimit() {
        return humLimitData;
    }

    //Getter, ktery pouziva aktivita
    public LiveData<String> getRealTemp() {
        return tempRealData;
    }

    // Enum pro status pripojeni
    enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
}
