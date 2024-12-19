package com.example.examen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialActivity extends AppCompatActivity {

    private ListView listViewRutas;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private List<String> rutasIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        listViewRutas = findViewById(R.id.listViewRutas);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        cargarRutas();

        listViewRutas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String rutaId = rutasIds.get(position);
                Intent intent = new Intent(HistorialActivity.this, DetalleRutaActivity.class);
                intent.putExtra("RUTA_ID", rutaId);
                startActivity(intent);
            }
        });
    }

    private void cargarRutas() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("rutas").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> rutas = new ArrayList<>();
                rutasIds = new ArrayList<>();

                for (DataSnapshot rutaSnapshot : dataSnapshot.getChildren()) {
                    String rutaId = rutaSnapshot.getKey();
                    Long timestamp = rutaSnapshot.child("fecha").getValue(Long.class);
                    Integer numPuntos = rutaSnapshot.child("puntos").getValue(Integer.class);
                    Float distancia = rutaSnapshot.child("distancia").getValue(Float.class);

                    if (timestamp != null && numPuntos != null && distancia != null) {
                        Date fecha = new Date(timestamp);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        String fechaFormateada = sdf.format(fecha);

                        String distanciaFormateada = String.format("%.2f", distancia / 1000);
                        rutas.add("Ruta del " + fechaFormateada + " - " + numPuntos + " puntos - " + distanciaFormateada + " km");
                        rutasIds.add(rutaId);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        HistorialActivity.this,
                        android.R.layout.simple_list_item_1,
                        rutas
                );
                listViewRutas.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(HistorialActivity.this,
                        "Error al cargar rutas: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}