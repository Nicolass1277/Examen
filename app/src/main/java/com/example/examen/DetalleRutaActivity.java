package com.example.examen;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DetalleRutaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private String rutaId;
    private TextView tvDistancia, tvDuracion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_ruta);

        rutaId = getIntent().getStringExtra("RUTA_ID");
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        tvDistancia = findViewById(R.id.tvDistancia);
        tvDuracion = findViewById(R.id.tvDuracion);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        cargarRuta();
    }

    private void cargarRuta() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("rutas").child(userId).child(rutaId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<LatLng> puntos = new ArrayList<>();
                for (DataSnapshot puntoSnapshot : dataSnapshot.getChildren()) {
                    double lat = puntoSnapshot.child("latitude").getValue(Double.class);
                    double lng = puntoSnapshot.child("longitude").getValue(Double.class);
                    puntos.add(new LatLng(lat, lng));
                }
                dibujarRuta(puntos);
                calcularEstadisticas(puntos);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejar error
            }
        });
    }

    private void dibujarRuta(List<LatLng> puntos) {
        PolylineOptions polylineOptions = new PolylineOptions().addAll(puntos);
        mMap.addPolyline(polylineOptions);
        if (!puntos.isEmpty()) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(puntos.get(0), 15));
        }
    }

    private void calcularEstadisticas(List<LatLng> puntos) {
        double distanciaTotal = 0;
        for (int i = 0; i < puntos.size() - 1; i++) {
            distanciaTotal += calcularDistancia(puntos.get(i), puntos.get(i + 1));
        }
        tvDistancia.setText(String.format("Distancia: %.2f km", distanciaTotal / 1000));

        // Suponiendo una velocidad promedio de 5 km/h para caminar
        double duracionHoras = distanciaTotal / 1000 / 5;
        int duracionMinutos = (int) (duracionHoras * 60);
        tvDuracion.setText(String.format("Duración estimada: %d minutos", duracionMinutos));
    }

    private double calcularDistancia(LatLng punto1, LatLng punto2) {
        double lat1 = punto1.latitude;
        double lon1 = punto1.longitude;
        double lat2 = punto2.latitude;
        double lon2 = punto2.longitude;
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515 * 1.609344 * 1000; // Convertir a metros
        return dist;
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
}