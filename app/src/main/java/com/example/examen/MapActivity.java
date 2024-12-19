package com.example.examen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Button btnIniciarRuta;
    private Button btnVerHistorial;
    private Button btnAgregarMarcador;
    private boolean rutaIniciada = false;
    private List<LatLng> puntos = new ArrayList<>();
    private DatabaseReference mDatabase;
    private String rutaActualId;
    private float distanciaTotal = 0;
    private Location ultimaUbicacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(MapActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        inicializarBotones();
        createLocationCallback();
    }

    private void inicializarBotones() {
        btnIniciarRuta = findViewById(R.id.btnIniciarRuta);
        btnVerHistorial = findViewById(R.id.btnVerHistorial);
        btnAgregarMarcador = findViewById(R.id.btnAgregarMarcador);

        btnIniciarRuta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!rutaIniciada) {
                    iniciarRuta();
                } else {
                    detenerRuta();
                }
            }
        });

        btnVerHistorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapActivity.this, HistorialActivity.class);
                startActivity(intent);
            }
        });

        btnAgregarMarcador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                agregarMarcador();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        solicitarPermisos();
    }

    private void solicitarPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            configurarMapa();
        }
    }

    private void configurarMapa() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));
            }
        });
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    LatLng punto = new LatLng(location.getLatitude(), location.getLongitude());
                    puntos.add(punto);
                    mMap.addPolyline(new PolylineOptions().addAll(puntos));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(punto));

                    // Calcular distancia
                    if (ultimaUbicacion != null) {
                        distanciaTotal += location.distanceTo(ultimaUbicacion);
                    }
                    ultimaUbicacion = location;

                    // Guardar en Firebase
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference coordenadaRef = mDatabase.child("rutas").child(userId).child(rutaActualId).child("coordenadas").push();
                    coordenadaRef.setValue(punto);

                    Log.d("MapActivity", "Nueva coordenada guardada: " + punto.toString());
                }
            }
        };
    }

    private void iniciarRuta() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            solicitarPermisos();
            return;
        }

        rutaIniciada = true;
        btnIniciarRuta.setText("Detener Ruta");
        puntos.clear();
        mMap.clear();
        distanciaTotal = 0;
        ultimaUbicacion = null;

        // Crear una nueva entrada para la ruta actual
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        rutaActualId = mDatabase.child("rutas").child(userId).push().getKey();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        Toast.makeText(this, "Ruta iniciada", Toast.LENGTH_SHORT).show();
        Log.d("MapActivity", "Iniciando ruta con ID: " + rutaActualId);
    }

    private void detenerRuta() {
        rutaIniciada = false;
        btnIniciarRuta.setText("Iniciar Ruta");
        fusedLocationClient.removeLocationUpdates(locationCallback);

        // Guardar información adicional de la ruta
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference rutaRef = mDatabase.child("rutas").child(userId).child(rutaActualId);
        rutaRef.child("fecha").setValue(ServerValue.TIMESTAMP);
        rutaRef.child("puntos").setValue(puntos.size());
        rutaRef.child("distancia").setValue(distanciaTotal);

        Toast.makeText(this, "Ruta guardada. Distancia: " + String.format("%.2f", distanciaTotal / 1000) + " km", Toast.LENGTH_SHORT).show();
        Log.d("MapActivity", "Deteniendo ruta con ID: " + rutaActualId + ", puntos: " + puntos.size() + ", distancia: " + distanciaTotal);
    }

    private void agregarMarcador() {
        if (mMap != null) {
            LatLng currentLocation = mMap.getCameraPosition().target;
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("Marcador"));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                configurarMapa();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mMap == null) {
            return false;
        }

        int itemId = item.getItemId();
        if (itemId == R.id.normal_map) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            return true;
        } else if (itemId == R.id.hybrid_map) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            return true;
        } else if (itemId == R.id.satellite_map) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            return true;
        } else if (itemId == R.id.terrain_map) {
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (rutaIniciada) {
            detenerRuta();
        }
    }
}