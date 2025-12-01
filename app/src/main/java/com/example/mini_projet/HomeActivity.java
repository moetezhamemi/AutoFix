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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private EditText searchBar;
    private CircleImageView profileIcon;
    private TextView locationText;
    private final Map<String, LatLng> cityMap = new HashMap<>();
    private static final int LOCATION_REQUEST = 1;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    private final List<Marker> garageMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Map initialization
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        searchBar = findViewById(R.id.search_bar);
        profileIcon = findViewById(R.id.profile_icon);
        ImageView chatListIcon = findViewById(R.id.chat_list_icon);
        locationText = findViewById(R.id.location_text);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPermission();
        setupSearch();
        loadUserProfile();

        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, profile.class);
            startActivity(intent);
        });

        chatListIcon.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ChatListActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Default location (Tunisia)
        LatLng tunisia = new LatLng(34.0, 9.0);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tunisia, 6.5f));

        initCityList();
        getUserLocation();

        // Check if location was shared from chat
        if (getIntent().hasExtra("sharedLatitude") && getIntent().hasExtra("sharedLongitude")) {
            double sharedLat = getIntent().getDoubleExtra("sharedLatitude", 0);
            double sharedLng = getIntent().getDoubleExtra("sharedLongitude", 0);
            displaySharedLocation(sharedLat, sharedLng);
        }
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

                    if (value != null && mMap != null) {
                        // Remove old garage markers
                        for (Marker marker : garageMarkers) {
                            marker.remove();
                        }
                        garageMarkers.clear();

                        for (DocumentSnapshot document : value.getDocuments()) {
                            Garage garage = document.toObject(Garage.class);
                            if (garage != null && garage.getAddress() != null) {
                                LatLng position = new LatLng(
                                        garage.getAddress().getLatitude(),
                                        garage.getAddress().getLongitude());

                                cityMap.put(garage.getName().toLowerCase(), position);

                                // Custom icon with rating text
                                Drawable defaultIcon = getResources().getDrawable(R.drawable.ic_garage_marker);
                                Bitmap customIcon = drawTextToBitmap(defaultIcon,
                                        String.format("⭐%.1f", garage.getRating()));

                                MarkerOptions markerOptions = new MarkerOptions()
                                        .position(position)
                                        .title(garage.getName())
                                        .snippet("Rating: " + String.format("%.1f", garage.getRating()) + " ⭐\n" +
                                                "Phone: " + garage.getPhone());

                                if (customIcon != null) {
                                    markerOptions.icon(BitmapDescriptorFactory.fromBitmap(customIcon));
                                } else {
                                    // Fallback if custom icon fails
                                    markerOptions.icon(
                                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                                }

                                Marker marker = mMap.addMarker(markerOptions);
                                marker.setTag(garage.getId()); // Store ID in tag
                                garageMarkers.add(marker);
                            }
                        }

                        // Set click listener to open garage details immediately when marker is clicked
                        mMap.setOnMarkerClickListener(marker -> {
                            String garageId = (String) marker.getTag();
                            if (garageId != null) {
                                Intent intent = new Intent(HomeActivity.this, GarageDetailActivity.class);
                                intent.putExtra("garageId", garageId);
                                startActivity(intent);
                                return true; // Consume the event
                            }
                            return false; // Let default behavior happen (e.g. for other markers)
                        });
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

            // Create paint for text
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK); // Text color
            paint.setTextSize(40); // Text size
            paint.setFakeBoldText(true);
            paint.setShadowLayer(5f, 0f, 0f, Color.WHITE); // White shadow for better visibility
            paint.setTextAlign(Paint.Align.CENTER); // Center text

            // Calculate dimensions
            float textWidth = paint.measureText(text);
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            float textHeight = fontMetrics.descent - fontMetrics.ascent;
            int padding = 10;

            int width = Math.max(mutableBitmap.getWidth(), (int) textWidth + 20);
            int height = mutableBitmap.getHeight() + (int) textHeight + padding + 10;

            Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalBitmap);

            // Draw the text at the top, centered
            // y is baseline, so we add -ascent to padding
            float textX = width / 2f;
            float textY = padding - fontMetrics.ascent;
            canvas.drawText(text, textX, textY, paint);

            // Draw the icon below the text, centered
            float iconX = (width - mutableBitmap.getWidth()) / 2f;
            float iconY = textHeight + padding + 5;
            canvas.drawBitmap(mutableBitmap, iconX, iconY, null);

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

        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
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
                                    LatLng userLocation = new LatLng(location.getLatitude(),
                                            location.getLongitude());
                                    if (mMap != null) {
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12.0f));
                                    }
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
        LatLng gp = cityMap.get(query);
        if (gp != null) {
            zoomTo(gp, query);
            return;
        }

        Geocoder gc = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = gc.getFromLocationName(query + ", Tunisia", 1);
            if (!addresses.isEmpty()) {
                Address a = addresses.get(0);
                gp = new LatLng(a.getLatitude(), a.getLongitude());
                zoomTo(gp, query);
                return;
            }
        } catch (IOException ignored) {
        }

        Toast.makeText(this, "Not found – try \"Tunis\" or \"Sousse\"", Toast.LENGTH_SHORT).show();
    }

    private void zoomTo(LatLng point, String title) {
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 12.0f));
        }
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

    private void displaySharedLocation(double latitude, double longitude) {
        if (mMap == null)
            return;

        LatLng sharedPoint = new LatLng(latitude, longitude);

        // Create red marker for shared location
        mMap.addMarker(new MarkerOptions()
                .position(sharedPoint)
                .title("Client Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Zoom to shared location
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sharedPoint, 15.0f));

        Toast.makeText(this, "Client location displayed", Toast.LENGTH_SHORT).show();
    }
}
