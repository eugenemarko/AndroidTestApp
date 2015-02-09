package com.eugenemarko.testapp;

import android.app.Activity;
import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.File;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;


public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public final static String EXTRA_MESSAGE = "com.eugenemarko.testapp.MESSAGE";

    private GoogleApiClient mGoogleApiClient;
    NfcAdapter mNfcAdapter;
    // List of URIs to provide to Android Beam
    private Uri[] mFileUris = new Uri[10];
    // Instance that returns available files from this app
    private FileUriCallback mFileUriCallback;
    private Location mLastLocation;
    private AddressResultReceiver mResultReceiver;
    private boolean mAddressRequested;
    private String mAddressOutput;
/*
    public MainActivity(AddressResultReceiver mResultReceiver) {
        this.mResultReceiver = mResultReceiver;
    }
*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sendFileButton = (Button) findViewById(R.id.sendFileButton);

        // NFC isn't available on the device
        //Disable NFC features here.
        // Android Beam file transfer isn't supported
        if (!isFeatureAvailable(this, PackageManager.FEATURE_NFC))
            sendFileButton.setVisibility(View.INVISIBLE);
        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            sendFileButton.setEnabled(false);
        else {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            mFileUriCallback = new FileUriCallback();
            mNfcAdapter.setBeamPushUrisCallback(mFileUriCallback,this);
        }

       buildGoogleApiClient();
       // TODO should initialize Receiver
       // mResultReceiver = new AddressResultReceiver((Handler)this);
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        Log.i("MainActivity", "Google api client builder. Connecting:" + String.valueOf(mGoogleApiClient.isConnecting()));
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                openSearch();
                return true;
            case R.id.action_settings:
                openSettings();
                return true;
            case R.id.action_notify:
                sendNotification();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendNotification() {


        int notificationId = 1;

        TestNotification.notify(this, "ets",notificationId );



    }

    private void openSearch() {
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Send button */
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void sendFile(View view) {

        String transferFile = "File.txt";
        File extDir = getExternalFilesDir(null);
        File requestFile = new File(extDir, transferFile);
        //requestFile.setReadable(true, false);
        // Get a URI for the File and add it to the list of URIs
        Uri fileUri = Uri.fromFile(requestFile);
        if (fileUri != null) {
            mFileUris[0] = fileUri;
        } else Log.e("Main Activity", "No File URI available for file.");
    }

    public void getLocation(View view) {
 /*       TextView locationText = (TextView) findViewById(R.id.locationTextView);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null)
            locationText.setText(String.valueOf(mLastLocation.getLatitude())+String.valueOf(mLastLocation.getLongitude()));*/

        // Only start the service to fetch the address if GoogleApiClient is
        // connected.
        Log.i("Main Activity","Google api connected: " + String.valueOf(mGoogleApiClient.isConnected()));
        if (mGoogleApiClient.isConnected() && mLastLocation != null) {
            startIntentService();
        }
        // If GoogleApiClient isn't connected, process the user's request by
        // setting mAddressRequested to true. Later, when GoogleApiClient connects,
        // launch the service to fetch the address. As far as the user is
        // concerned, pressing the Fetch Address button
        // immediately kicks off the process of getting the address.
        mAddressRequested = true;
        // TODO define update widgets
        //updateUIWidgets();
    }

    public static boolean isFeatureAvailable(Context context, String feature) {
        final PackageManager packageManager = context.getPackageManager();
        final FeatureInfo[] featuresList = packageManager.getSystemAvailableFeatures();
        for (FeatureInfo f : featuresList) {
            if (f.name != null && f.name.equals(feature)) {
                return true;
            }
        }

        return false;

    }

    @Override
    public void onConnected(Bundle bundle) {
        // Gets the best and most recent location currently available,
        // which may be null in rare cases when a location is not available.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (mLastLocation != null) {
            // Determine whether a Geocoder is available.
            if (!Geocoder.isPresent())
                return;

            if (mAddressRequested)
                startIntentService();

        }
        Log.i("Main Activity", "onConnected");

    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
            Log.i("Main Activity", "AddressResultReceiver");
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                Log.i("Main Activity", "address found");
                //showToast(getString(R.string.address_found));
            }

        }
    }


    private void displayAddressOutput() {

        TextView locationText = (TextView) findViewById(R.id.locationTextView);
        locationText.setText(mAddressOutput);
        Log.i("Main Activity", "displayAddressOutput");


    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("Main Activity", "onConnectionSuspended");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("Main Activity", "onConnectionFailed");

    }

    private class FileUriCallback implements
                NfcAdapter.CreateBeamUrisCallback {
            public FileUriCallback() {
            }

            /**
             * Create content URIs as needed to share with another device
             */
            @Override
            public Uri[] createBeamUris(NfcEvent event) {
                return mFileUris;
            }
        }


    public void showMap(View view) {
        Log.i("Main Activity", "showMap method");
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }
}
