package com.example.monitorheart;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SymptomDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "symptoms.db";
    public static final int DATABASE_VERSION = 1;
    public SymptomDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS symptoms (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Heart_Rate FLOAT, " +
                "Respiratory_Rate FLOAT, " +
                "nausea FLOAT, " +
                "headache FLOAT, " +
                "diarrhea FLOAT, " +
                "sorethroat FLOAT, " +
                "fever FLOAT, " +
                "muscleache FLOAT, " +
                "loss_of_smell_or_taste FLOAT, " +
                "cough FLOAT, " +
                "shortness_of_breath FLOAT, " +
                "feeling_tired FLOAT);";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades here
    }
    public void updateSymptomRatings(Float[] ratings) {
        SQLiteDatabase db = getWritableDatabase();

        // Ensure that there is only one row in the table

        // Insert the updated ratings
        String insertQuery = "INSERT INTO symptoms (Heart_Rate, Respiratory_Rate, nausea, headache, diarrhea, sorethroat, fever, muscleache, loss_of_smell_or_taste, cough, shortness_of_breath, feeling_tired) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        db.execSQL(insertQuery, ratings);

        db.close();
    }
}
