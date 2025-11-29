package com.example.mini_projet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class home extends AppCompatActivity {
    private MapView map;
    private EditText searchBar;
    private final Map<String, GeoPoint> cityMap = new HashMap<>();
    private static final int LOCATION_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid cache folder
        Configuration.getInstance().load(getApplicationContext(),
                getPreferences(MODE_PRIVATE));

        setContentView(R.layout.activity_home);

        map = findViewById(R.id.map);
        searchBar = findViewById(R.id.search_bar);

        initMap();
        initCityList();
        requestLocationPermission();
        setupSearch();
        ImageButton profileButton = findViewById(R.id.profile_icon);
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(home.this, profile.class);
            startActivity(intent);
        });
    }

    private void initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(6.5);
        map.getController().setCenter(new GeoPoint(34.0, 9.0));

        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) { return false; }
            @Override public boolean longPressHelper(GeoPoint p) { return false; }
        }));
    }

    private void initCityList() {
        cityMap.put("tunis", new GeoPoint(36.8065, 10.1815));
        cityMap.put("ariana", new GeoPoint(36.8665, 10.1647));
        cityMap.put("hammamet", new GeoPoint(36.3743, 10.6167));
        cityMap.put("sousse", new GeoPoint(35.8375, 10.6333));
        cityMap.put("mahdia", new GeoPoint(35.6722, 10.0972));
        cityMap.put("el jem", new GeoPoint(35.0333, 11.0333));
        cityMap.put("sfax", new GeoPoint(34.7351, 10.7605));
        cityMap.put("kasserine", new GeoPoint(35.1722, 8.8305));
        cityMap.put("sidi bouzid", new GeoPoint(35.0382, 9.4839));
        cityMap.put("gabes", new GeoPoint(33.8815, 10.0982));

        for (Map.Entry<String, GeoPoint> e : cityMap.entrySet()) {
            Marker m = new Marker(map);
            m.setPosition(e.getValue());
            m.setTitle(e.getKey().substring(0, 1).toUpperCase() + e.getKey().substring(1));
            map.getOverlays().add(m);
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission granted – you can enable my-location overlay here if you want
        }
    }

    private void setupSearch() {
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String q = v.getText().toString().trim().toLowerCase(Locale.ROOT);
                performSearch(q);
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        GeoPoint gp = cityMap.get(query);
        if (gp != null) {
            zoomTo(gp, query);
            return;
        }

        Geocoder gc = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = gc.getFromLocationName(query + ", Tunisia", 1);
            if (!addresses.isEmpty()) {
                Address a = addresses.get(0);
                gp = new GeoPoint(a.getLatitude(), a.getLongitude());
                zoomTo(gp, query);
                return;
            }
        } catch (IOException ignored) {}

        Toast.makeText(this, "Not found – try \"Tunis\" or \"Sousse\"", Toast.LENGTH_SHORT).show();
    }

    private void zoomTo(GeoPoint point, String title) {
        map.getController().animateTo(point);
        map.getController().setZoom(12.0);

        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setTitle(title.substring(0, 1).toUpperCase() + title.substring(1));
        marker.setIcon(getResources().getDrawable(android.R.drawable.star_big_on));
        map.getOverlays().add(marker);
        map.invalidate();
    }

    @Override public void onResume() { super.onResume(); map.onResume(); }
    @Override public void onPause()  { super.onPause();  map.onPause();  }
}
