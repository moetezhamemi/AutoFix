package com.example.mini_projet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import de.hdodenhof.circleimageview.CircleImageView;
import com.example.mini_projet.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

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
    private CircleImageView profileIcon;
    private TextView locationText;
    private final Map<String, GeoPoint> cityMap = new HashMap<>();
    private static final int LOCATION_REQUEST = 1;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid cache folder
        Configuration.getInstance().load(getApplicationContext(),
                getPreferences(MODE_PRIVATE));

        setContentView(R.layout.activity_home);

        map = findViewById(R.id.map);
        searchBar = findViewById(R.id.search_bar);
        profileIcon = findViewById(R.id.profile_icon);
        ImageView chatListIcon = findViewById(R.id.chat_list_icon);
        locationText = findViewById(R.id.location_text);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initMap();
        initCityList();
        requestLocationPermission();
        setupSearch();
        loadUserProfile();
        getUserLocation();

        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(home.this, profile.class);
            startActivity(intent);
        });

        chatListIcon.setOnClickListener(v -> {
            Intent intent = new Intent(home.this, ChatListActivity.class);
            startActivity(intent);
        });
    }

    private void initMap() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(6.5);
        map.getController().setCenter(new GeoPoint(34.0, 9.0));

        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
    }

    private void initCityList() {
        // Fetch approved garages from Firestore
        db.collection("garages")
                .whereEqualTo("enabled", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                            com.example.mini_projet.models.Garage garage = document
                                    .toObject(com.example.mini_projet.models.Garage.class);
                            if (garage != null && garage.getAddress() != null) {
                                // Convert Firestore GeoPoint to OSMDroid GeoPoint
                                GeoPoint geoPoint = new GeoPoint(
                                        garage.getAddress().getLatitude(),
                                        garage.getAddress().getLongitude());

                                // Add to cityMap for search functionality
                                cityMap.put(garage.getName().toLowerCase(), geoPoint);

                                // Create marker for the garage
                                Marker marker = new Marker(map);
                                marker.setPosition(geoPoint);
                                marker.setTitle(garage.getName() + " ⭐ " + String.format("%.1f", garage.getRating()));
                                marker.setSnippet("Phone: " + garage.getPhone() + "\n" + garage.getDescription());
                                marker.setIcon(getResources().getDrawable(R.drawable.ic_garage_marker));

                                // Set click listener to open garage details
                                marker.setOnMarkerClickListener((m, mapView) -> {
                                    Intent intent = new Intent(home.this, GarageDetailActivity.class);
                                    intent.putExtra("garageId", garage.getId());
                                    startActivity(intent);
                                    return true;
                                });

                                map.getOverlays().add(marker);
                            }
                        }
                        map.invalidate(); // Refresh the map
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading garages: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
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
            // Permission accordée, récupérer la localisation
            getUserLocation();
        }
    }

    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Récupérer le nom de la ville via Geocoder
                            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                            try {
                                List<Address> addresses = geocoder.getFromLocation(
                                        location.getLatitude(),
                                        location.getLongitude(),
                                        1);
                                if (addresses != null && !addresses.isEmpty()) {
                                    Address address = addresses.get(0);
                                    String city = address.getLocality();
                                    String country = address.getCountryName();

                                    if (city != null && country != null) {
                                        locationText.setText(country + ", " + city);
                                    } else if (city != null) {
                                        locationText.setText(city);
                                    } else if (country != null) {
                                        locationText.setText(country);
                                    }

                                    // Centrer la carte sur la position actuelle
                                    GeoPoint userLocation = new GeoPoint(location.getLatitude(),
                                            location.getLongitude());
                                    map.getController().setCenter(userLocation);
                                    map.getController().setZoom(12.0);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Impossible de récupérer l'adresse", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Impossible de récupérer la localisation", Toast.LENGTH_SHORT).show();
                    });
        } catch (SecurityException e) {
            e.printStackTrace();
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
        } catch (IOException ignored) {
        }

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

    private void loadUserProfile() {
        if (auth.getCurrentUser() == null) {
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                            Picasso.get()
                                    .load(user.getPhotoUrl())
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .into(profileIcon);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // En cas d'erreur, garder l'image par défaut
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}
