package ee.sinchukov.ecbrates;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class CurrenciesListActivity extends ListActivity {
    private static final String TAG = "MainActivity";
    private static final String SETTINGS_NAME = "ecbrates_app_settings";
    private static final String PREFS_XML_DATE = "parsed_xml_date";

    private String xmlDateFromPrefs;
    private SimpleDateFormat dateFormatter;
    public static String newLine = System.getProperty("line.separator");

    private static final String FILENAME = "saved_Ecb_Rates_data.xml";
    private static final String ecbUpdatingTime = "15:00:00";
    private static final int ecbUpdatePeriodHours = 24;
    private static final String ecbRatesTimezone = "CET";
    private static final String dateFormat = "yyyy-MM-dd HH:mm:ss";
    private static final String ecbRatesDefaultDate = "2015-06-01";
    private  static final String ecbRateUrl = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

    private HandleXML handleXML;

    // for sql lite
    DBHelper dbHelper;
    public static String tableName = "ecbRatesTable";
    public String toastText="text";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dateFormatter = new SimpleDateFormat(dateFormat);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(ecbRatesTimezone));

        //date of last saved currency rates
        xmlDateFromPrefs = getDataFromApplicationSettings(PREFS_XML_DATE);
        if(xmlDateFromPrefs.equals("not found")){xmlDateFromPrefs=ecbRatesDefaultDate;}

        // prepare for XML parsing
        handleXML=new HandleXML(ecbRateUrl, this);

        // if rates are not fresh, then get new from URL and save xml file to internal storage
        if(!isFreshRates(xmlDateFromPrefs+" "+ecbUpdatingTime)) {
            handleXML.xmlFromUrlToString();
            while (handleXML.getXmlStringFromUrlNonComplete) ;
            writeToFile(handleXML.getXmlStringDataFromUrl(), FILENAME);
        }

        // parse xml from internal storage
        handleXML.fetchXmlFromInternalStorage(FILENAME);
        while(handleXML.parsingNonComplete);
        saveDataToApplicationSettings(PREFS_XML_DATE,handleXML.getReceivedXmlDate());

        // display rates on screen
        String[] from=new String[] { Cube.CURRENCY, Cube.RATE };
        int[] to=new int[] {R.id.currencyView, R.id.rateView };
        ListAdapter adapter = new SimpleAdapter(this, handleXML.getCubeList(), R.layout.activity_currencies_list,from,to);
        setListAdapter(adapter);

        // создаем объект для создания и управления версиями БД
        Log.d(TAG, "--- create DB Helper ");
        dbHelper = new DBHelper(this);

        // clean table and show message
        cleanRatesInDB();

        // save rates to database and show message
        insertRatesToDB();

        // read rates from database and show message
        getRatesFromDB();
    }

    private boolean isFreshRates(String savedXmlDate){
        final int MILLI_TO_HOUR = 1000 * 60 * 60;
        Date currentDate = new Date();
        Date savedDate=null;

        // parse date of local saved ecb rates xml file
        try {
            savedDate = dateFormatter.parse(savedXmlDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //find what weekday it was
        SimpleDateFormat weekDayFormat = new SimpleDateFormat("EEEE", Locale.US);
        String dayOfUpdate = weekDayFormat.format(savedDate);

        // find when we will have next update
        int nextUpdate = (int) savedDate.getTime() + (ecbUpdatePeriodHours*MILLI_TO_HOUR);

        // if last update was in Friday then next update will be on Monday, i.e. +48 hours
        if(dayOfUpdate.equalsIgnoreCase("Friday")){
                nextUpdate = nextUpdate + (48*MILLI_TO_HOUR);
            }

        // difference between current time and time of next update
        int difference = (int) currentDate.getTime() - nextUpdate;

        if(difference >0){
            // local saved rates are old,  need to update from URL
            return false;}
        else{
            // local saved rates are fresh, use xml from internal storage
            return true;}

    }

    private void writeToFile(String data, String fileName) {
        try {
            OutputStreamWriter outputStreamWriter = new
                    OutputStreamWriter(openFileOutput(fileName, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }
    }

    public void saveDataToApplicationSettings(String key, String value){

        // save key-value to application settings
        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key,value);
        editor.commit();

    }

    public String getDataFromApplicationSettings(String key){

        String value;
        // save key-value to application settings
        SharedPreferences settings = getSharedPreferences(SETTINGS_NAME, 0);
        value = settings.getString(key, "not found");
        return value;
    }



    public void cleanRatesInDB(){
        // создаем объект для данных
        ContentValues cv = new ContentValues();

        // подключаемся к БД
        Log.d(TAG, "--- try dbHelper.getWritableDatabase ");
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Log.d(TAG, "--- Clear table ---");
        // удаляем все записи
        int clearCount = db.delete(tableName, null, null);
        Log.d(TAG, "deleted rows count = " + clearCount);

        showAlert("Done. "+ "Deleted rows = " + clearCount +" Table "+tableName+ "is clean.",this);
        // закрываем подключение к БД
        dbHelper.close();
    }


    public void insertRatesToDB(){
        // создаем объект для данных
        ContentValues cv = new ContentValues();

        // подключаемся к БД
        Log.d(TAG, "--- try dbHelper.getWritableDatabase ");
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Log.d(TAG, "--- Insert into table: ---");
        // подготовим данные для вставки в виде пар: наименование столбца - значение

        int counter=0;
        for(Cube cube: handleXML.getCubeList()) {
            cv.put("currency", cube.get(Cube.CURRENCY));
            cv.put("rate", cube.get(Cube.RATE));
            // вставляем запись и получаем ее ID
            long rowID = db.insert(tableName, null, cv);
            Log.d(TAG, "row inserted, ID = " + rowID);
            counter++;
        }
        showAlert("inserted "+counter+ " rows into "+tableName+ " table",this);
        // закрываем подключение к БД
        dbHelper.close();
    }

    public void getRatesFromDB(){
        // создаем объект для данных
        ContentValues cv = new ContentValues();

        // подключаемся к БД
        Log.d(TAG, "--- try dbHelper.getWritableDatabase ");
        SQLiteDatabase db = dbHelper.getWritableDatabase();



        Log.d(TAG, "--- read rows from table: ---");
        // делаем запрос всех данных из таблицы, получаем Cursor
        Cursor c = db.query(tableName, null, null, null, null, null, null);

        int counter=0;
        StringBuilder message = new StringBuilder();
        // ставим позицию курсора на первую строку выборки
        // если в выборке нет строк, вернется false
        if (c.moveToFirst()) {
            // определяем номера столбцов по имени в выборке
            int idColIndex = c.getColumnIndex("id");
            int currencyColIndex = c.getColumnIndex("currency");
            int rateColIndex = c.getColumnIndex("rate");

            do {
                counter++;
                // получаем значения по номерам столбцов и пишем в лог
                message.append("ID = " + c.getInt(idColIndex) +
                        ", currency = " + c.getString(currencyColIndex) +
                        ", rate = " + c.getString(rateColIndex));
                message.append(newLine);

                // получаем значения по номерам столбцов и пишем в лог
                Log.d(TAG,
                        "ID = " + c.getInt(idColIndex) +
                                ", name = " + c.getString(currencyColIndex) +
                                ", email = " + c.getString(rateColIndex));
                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (c.moveToNext());
        } else
            Log.d(TAG, "0 rows");
        c.close();

        message.append("Done. Read "+counter+ " rows from table "+tableName);
        showAlert(message.toString(),this);

        // закрываем подключение к БД
        dbHelper.close();
    }



    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            // конструктор суперкласса
            super(context, "myDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d("MainActivity", "--- onCreate database ---");
            // создаем таблицу с полями
            db.execSQL("create table "+ tableName +" ("
                    + "id integer primary key autoincrement,"
                    + "currency text,"
                    + "rate text" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    public static void showAlert(String message, Context con) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(con);
        dialog.setMessage(message);
        dialog.setPositiveButton(" OK ", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();

            }
        });
        dialog.show();

    }

}
