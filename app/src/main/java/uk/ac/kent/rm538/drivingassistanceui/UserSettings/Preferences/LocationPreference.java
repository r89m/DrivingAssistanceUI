package uk.ac.kent.rm538.drivingassistanceui.UserSettings.Preferences;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;
import uk.ac.kent.rm538.drivingassistanceui.DrivingAssistanceApp;
import uk.ac.kent.rm538.drivingassistanceui.R;

public class LocationPreference extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private static double DEFAULT_LATITUDE = 51.508179;
    private static double DEFAULT_LONGITUDE = -0.128037;
    private static int DEFAULT_ZOOM_LEVEL = 15;


    private GoogleMap mMap;

    private double latitude;
    private double longitude;

    @Bind(R.id.set_location_accept) Button acceptButton;
    @Bind(R.id.set_location_cancel) Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_preference);

        // Start a runnable that does the entire Map setup.
        // A delay does not seem to be necessary.
        Handler mapSetupHandler = new Handler();
        Runnable mapSetupRunnable = new Runnable() {
            public void run() {
                FragmentManager fragMan = getSupportFragmentManager();
                FragmentTransaction fragTransaction = fragMan.beginTransaction();

                final SupportMapFragment mapFragment = new SupportMapFragment();
                fragTransaction.add(R.id.map_container, mapFragment);
                fragTransaction.commit();

                // At the end, retrieve the GoogleMap object and the View for further setup,
                // zoom in on a region, add markers etc.
                mapFragment.getMapAsync(LocationPreference.this);
            }
        };
        mapSetupHandler.post(mapSetupRunnable);

        ButterKnife.bind(this);

        acceptButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);

        Intent mapIntent = getIntent();
        latitude = mapIntent.getDoubleExtra(DrivingAssistanceApp.INTENT_EXTRA_LOCATION_LATITUDE, 0);
        longitude = mapIntent.getDoubleExtra(DrivingAssistanceApp.INTENT_EXTRA_LOCATION_LONGITUDE, 0);

        if(latitude == 0){
            latitude = DEFAULT_LATITUDE;
        }
        if(longitude == 0){
            longitude = DEFAULT_LONGITUDE;
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng activationLocation = new LatLng(latitude, longitude);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(activationLocation)
                .zoom(DEFAULT_ZOOM_LEVEL)
                .build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.set_location_accept:

                if(mMap != null) {
                    LatLng position = mMap.getCameraPosition().target;
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(DrivingAssistanceApp.INTENT_EXTRA_LOCATION_LATITUDE, position.latitude);
                    resultIntent.putExtra(DrivingAssistanceApp.INTENT_EXTRA_LOCATION_LONGITUDE, position.longitude);
                    returnResult(RESULT_OK, resultIntent);
                } else {
                    returnResult(RESULT_CANCELED);
                }
                break;

            case R.id.set_location_cancel:
                returnResult(RESULT_CANCELED);
                break;

        }

    }

    private void returnResult(int result) {

        returnResult(result, null);
    }

    private void returnResult(int result, Intent data){

        if(getParent() == null){
            setResult(result, data);
        } else {
            getParent().setResult(result, data);
        }
        finish();
    }
}
