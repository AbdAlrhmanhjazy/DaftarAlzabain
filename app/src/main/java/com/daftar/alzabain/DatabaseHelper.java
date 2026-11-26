package com.daftar.alzabain;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "daftar_alzabain.db";
    private static final int DB_VERSION = 3;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE customers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT, party_type TEXT DEFAULT 'CUSTOMER', note TEXT)");
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_id INTEGER NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, note TEXT, created_at TEXT, FOREIGN KEY(customer_id) REFERENCES customers(id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN created_at TEXT");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE customers ADD COLUMN party_type TEXT DEFAULT 'CUSTOMER'");
            db.execSQL("ALTER TABLE customers ADD COLUMN note TEXT");
        }
    }
}