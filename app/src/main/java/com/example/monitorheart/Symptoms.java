package com.example.monitorheart;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Objects;

public class Symptoms extends AppCompatActivity {
    private Spinner symptomsDropdown;
    private RatingBar rating;
    private Button uplBtn;
    private TextView txtView;
    ArrayList<Map<String, Object>> symptomRatingList = new ArrayList<>();
    private SymptomDatabaseHelper dbHelper;
    private Float[] ratings = new Float[12];

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptoms);
        symptomsDropdown = findViewById(R.id.symptomSpinner);
        rating = findViewById(R.id.ratingBar);
        uplBtn = findViewById(R.id.button3);
        txtView = findViewById(R.id.textView3);
        String[] symptoms = {"Select a symptom","Nausea", "Headache", "Diarrhea", "SoreThroat", "Fever", "MuscleAche", "Loss of Smell or Taste", "Cough", "Shortness of Breath", "Feeling Tired"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, symptoms);
        symptomsDropdown.setAdapter(adapter);
        ratings[0] = (float) getIntent().getIntExtra("heartRate", 52);
        ratings[1] = (float) getIntent().getIntExtra("respiratoryRate", 77);
        Log.wtf("symptom", String.valueOf(ratings[0]));
        Log.wtf("symptom", String.valueOf(ratings[1]));
        dbHelper = new SymptomDatabaseHelper(this);


        for (String symptom : symptoms) {
            if(Objects.equals(symptom, "Select a symptom"))
            {
                continue;
            }
            Map<String, Object> symptomData = new HashMap<>();
            symptomData.put("SymptomName", symptom);
            symptomData.put("Rating", 0f);
            symptomRatingList.add(symptomData);
        }

        rating.setOnRatingBarChangeListener((ratingBar, v, b) -> {
            if (symptomsDropdown.getSelectedItemPosition() == 0) {
                return;
            }
            int selectedSymptomPosition = symptomsDropdown.getSelectedItemPosition();
            String selectedSymptom = symptoms[selectedSymptomPosition];
            float selectedRating = rating.getRating();

            for (Map<String, Object> symptomData : symptomRatingList) {
                String name = (String) symptomData.get("SymptomName");
                if (name.equals(selectedSymptom)) {
                    symptomData.put("Rating", selectedRating);
                    break;
                }
            }

            StringBuilder displayText = new StringBuilder();

            for (Map<String, Object> symptomData : symptomRatingList) {
                String symptom = (String) symptomData.get("SymptomName");
                Object rating = symptomData.get("Rating");
                displayText.append("Symptom: ").append(symptom).append("\n").append("Rating: ").append(rating).append("\n");
            }
            txtView.setText("");
            txtView.setText(displayText.toString());
            symptomsDropdown.setSelection(0);
            ratingBar.setRating(0);
        });
        uplBtn.setOnClickListener(view -> uploadData());
    }
    private void uploadData() {
        int i = 2;
        for (Map<String, Object> symptomData : symptomRatingList) {

            String symptom = (String) symptomData.get("SymptomName");
            Object rating = symptomData.get("Rating");
            if (!symptom.equals("Select a symptom") && i != 12) {
                    ratings[i] = (Float) rating;
                    i += 1;
            }
        }

        dbHelper.updateSymptomRatings((ratings));
        Toast.makeText(this, "Data Inserted Successfully", Toast.LENGTH_SHORT).show();
        txtView.setText("Symptoms Selected");
    }
}
