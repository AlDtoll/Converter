package com.example.pusika.converter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Xml;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    EditText sumEditText;
    Spinner firstCurrencySpinner;
    Spinner secondCurrencySpinner;
    TextView contentView;
    DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sumEditText = findViewById(R.id.sumEditText);
        contentView = findViewById(R.id.textView);
        dbHelper = new DBHelper(this);
        firstCurrencySpinner = findViewById(R.id.firstCurrencySpinner);
        firstCurrencySpinner.setPrompt("Исходная валюта");
        secondCurrencySpinner = findViewById(R.id.secondCurrencySpinner);
        secondCurrencySpinner.setPrompt("Конечная валюта");
        if (hasConnection(this)) {
            contentView.setText("Загрузка данных");
            ProgressTask progressTask = new ProgressTask();
            progressTask.execute("http://www.cbr.ru/scripts/XML_daily.asp");
        } else {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor c = db.query("mytable", null, null, null, null, null, null);
            String content;
            String date;
            String message = "Нет соединения с сетью";
            if (c.moveToFirst()) {
                int contentColIndex = c.getColumnIndex("content");
                int dateColIndex = c.getColumnIndex("date");
                do {
                    content = c.getString(contentColIndex);
                    date = c.getString(dateColIndex);
                    createSpinnersForValutes(content);
                    message = "Нет соединения с сетью. Будут использованы данные за " + date;
                } while (c.moveToNext());
            } else {
                c.close();
            }
            contentView.setText(message);
        }
    }

    public void convert(View view) {
        double sum = Double.valueOf(sumEditText.getText().toString());
        Valute firstValute = (Valute) firstCurrencySpinner.getSelectedItem();
        Valute secondValute = ((Valute) secondCurrencySpinner.getSelectedItem());
        TextView result = findViewById(R.id.resultOfConvertTextView);
        String formattedDouble = new DecimalFormat("#0.0000").format(sum * firstValute.getNominal() / firstValute.getValue() * secondValute.getValue() / secondValute.getNominal());
        result.setText(formattedDouble);
    }

    public class ProgressTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... path) {
            String content;
            try {
                content = getContent(path[0]);
            } catch (IOException ex) {
                content = ex.getMessage();
            }
            return content;
        }

        @Override
        protected void onPostExecute(String content) {
            createSpinnersForValutes(content);
            contentView.setText("Загрузка данных завершена");
            ContentValues cv = new ContentValues();
            cv.put("content", content);
            cv.put("date", new Date().toString());
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.insert("mytable", null, cv);
        }

        private String getContent(String path) throws IOException {
            BufferedReader reader = null;
            try {
                URL url = new URL(path);
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                c.setReadTimeout(10000);
                c.connect();
                reader = new BufferedReader(new InputStreamReader(c.getInputStream(), "cp1251"));
                StringBuilder buf = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line).append("\n");
                }
                return buf.toString();
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }

    public void createSpinnersForValutes(String content) {
        content = content.substring(content.indexOf("<ValCurs"),content.indexOf("</ValCurs>"));
        XmlPullParser xmlPullParser = Xml.newPullParser();
        try {
            xmlPullParser.setInput(new StringReader(content));
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        ArrayList<Valute> list = new ArrayList<>();
        try {
            ValuteResourseParser valuteResourseParser = new ValuteResourseParser();
            if (valuteResourseParser.parse(xmlPullParser)) {
                list.addAll(valuteResourseParser.getValutes());
                list.add(new Valute(810, "RUR", 1, "Российский рубль", 1));
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
        setAdapterForSpinner(firstCurrencySpinner, list);
        setAdapterForSpinner(secondCurrencySpinner, list);
    }

    private void setAdapterForSpinner(final Spinner spinner, ArrayList list) {
        // адаптер
        ArrayAdapter<Valute> adapter = new ArrayAdapter<Valute>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private static boolean hasConnection(final Context context) {
        //todo Проверка разрешений
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = connectivityManager.getActiveNetworkInfo();
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        return false;
    }

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            // конструктор суперкласса
            super(context, "myDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table mytable ("
                    + "id integer primary key autoincrement,"
                    + "content text,"
                    + "date Date" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
