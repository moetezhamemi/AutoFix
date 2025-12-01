package com.example.mini_projet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mini_projet.models.Garage;
import com.example.mini_projet.models.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

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

    private final List<Marker> garageMarkers = new ArrayList<>();

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

        // Check if location was shared from chat
        if (getIntent().hasExtra("sharedLatitude") && getIntent().hasExtra("sharedLongitude")) {
            double sharedLat = getIntent().getDoubleExtra("sharedLatitude", 0);
            double sharedLng = getIntent().getDoubleExtra("sharedLongitude", 0);
            displaySharedLocation(sharedLat, sharedLng);
        }

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
        // Fetch approved garages from Firestore with real-time updates
        db.collection("garages")
                .whereEqualTo("enabled", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading garages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        // Remove old garage markers
                        map.getOverlays().removeAll(garageMarkers);
                        garageMarkers.clear();

                        for (DocumentSnapshot document : value.getDocuments()) {
                            Garage garage = document.toObject(Garage.class);
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
                                marker.setTitle(garage.getName());
                                marker.setSnippet("Rating: " + String.format("%.1f", garage.getRating()) + " ⭐\n" +
                                        "Phone: " + garage.getPhone() + "\n" + garage.getDescription());

                                // Custom icon with rating text
                                Drawable defaultIcon = getResources().getDrawable(R.drawable.ic_garage_marker);
                                Bitmap customIcon = drawTextToBitmap(defaultIcon,
                                        String.format("⭐%.1f", garage.getRating()));
                                if (customIcon != null) {
                                    marker.setIcon(new BitmapDrawable(getResources(), customIcon));
                                } else {
                                    marker.setIcon(defaultIcon);
                                }

                                // Set click listener to open garage details
                                marker.setOnMarkerClickListener((m, mapView) -> {
                                    Intent intent = new Intent(home.this, GarageDetailActivity.class);
                                    intent.putExtra("garageId", garage.getId());
                                    startActivity(intent);
                                    return true;
                                });

                                map.getOverlays().add(marker);
                                garageMarkers.add(marker);
                            }
                        }
                        map.invalidate(); // Refresh the map
                    }
                });
    }

    private Bitmap drawTextToBitmap(Drawable drawable, String text) {
        try {
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }

            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            // Create a larger bitmap to hold both icon and text
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK); // Text color
            paint.setTextSize(40); // Text size
            paint.setFakeBoldText(true);
            paint.setShadowLayer(5f, 0f, 0f, Color.WHITE); // White shadow for better visibility

            // Calculate text size
            float textWidth = paint.measureText(text);
            int width = mutableBitmap.getWidth() + (int) textWidth + 20; // Add padding
            int height = Math.max(mutableBitmap.getHeight(), 60); // Ensure enough height

            Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalBitmap);

            // Draw the icon
            canvas.drawBitmap(mutableBitmap, 0, (height - mutableBitmap.getHeight()) / 2f, null);

            // Draw the text
            canvas.drawText(text, mutableBitmap.getWidth() + 10, (height + 30) / 2f, paint);

            return finalBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

                                    // Add green marker for user's current position
                                    Marker userMarker = new Marker(map);
                                    userMarker.setPosition(userLocation);
                                    userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                    userMarker.setTitle("Your Location");
                                    userMarker.setSnippet("You are here");
                                    userMarker.setIcon(getResources().getDrawable(R.drawable.ic_user_location_marker));
                                    map.getOverlays().add(userMarker);
                                    map.invalidate();
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

    private void displaySharedLocation(double latitude, double longitude) {
        GeoPoint sharedPoint = new GeoPoint(latitude, longitude);

        // Create red marker for shared location
        Marker sharedMarker = new Marker(map);
        sharedMarker.setPosition(sharedPoint);
        sharedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        sharedMarker.setTitle("Client Location");

        // Set red color for the marker
        sharedMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_dialog_map));
        sharedMarker.setTextIcon("📍"); // Red pin emoji

        map.getOverlays().add(sharedMarker);

        // Zoom to shared location
        map.getController().setZoom(15.0);
        map.getController().setCenter(sharedPoint);

        Toast.makeText(this, "Client location displayed", Toast.LENGTH_SHORT).show();
    }
}
