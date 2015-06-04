package ee.sinchukov.ecbrates;

/**
 * Created by vsinchuk on 5/15/2015.
 */


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class HandleXML {
    Context context;
    private String urlString = null;
    public volatile boolean parsingNonComplete = true;
    public volatile boolean getXmlStringFromUrlNonComplete = true;

    private XmlPullParserFactory xmlFactoryObject;

    ArrayList<Cube> cubeList = new ArrayList<>();
    private String xmlStringDataFromUrl = "string builded from parsed XML";
    private String receivedXmlDate = "not found";
    private static final String TAG = "MainActivity";

    // for sql lite
    DBHelper dbHelper;
    public static String tableName = "ecbRatesTable";

    public HandleXML(String url,Context context){
        super();
        this.urlString = url;
        this.context = context;
    }

    public String getXmlStringDataFromUrl(){
        return xmlStringDataFromUrl;
    }

    public String getReceivedXmlDate(){
        return receivedXmlDate;
    }
    public ArrayList<Cube> getCubeList(){
        return cubeList;
    }

    public void populateCurrencyList(XmlPullParser ecbParser) {

        try {

            int eventType = ecbParser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType==XmlPullParser.START_TAG) {
                    if(ecbParser.getName().equals("Cube")&& ecbParser.getAttributeCount()==1){
                        //save xml date into String
                        receivedXmlDate=ecbParser.getAttributeValue(0);
                    }

                    if(ecbParser.getName().equals("Cube")&& ecbParser.getAttributeCount()==2) {
                        // create Cube object and save into array
                        cubeList.add(new Cube(ecbParser.getAttributeValue(0), ecbParser.getAttributeValue(1)));
                    }
                }
                eventType = ecbParser.next();
            }
            parsingNonComplete = false;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void xmlFromUrlToString(){
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection)
                            url.openConnection();
                    conn.setReadTimeout(10000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream stream = conn.getInputStream();

                    // start build String from input stream
                    try {
                        if ( stream != null ) {
                            InputStreamReader inputStreamReader = new InputStreamReader(stream);
                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                            String receiveString = "";
                            StringBuilder stringBuilder = new StringBuilder();
                            while ( (receiveString = bufferedReader.readLine()) != null ) {
                                stringBuilder.append(receiveString);
                            }
                            //stream.close();
                            xmlStringDataFromUrl = stringBuilder.toString();
                            getXmlStringFromUrlNonComplete = false;
                        }
                    }
                    catch (IOException e) {
                        Log.e(TAG, "Can not build String from input stream: " + e.toString());
                    } finally {
                        stream.close();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }

    public void fetchXmlFromInternalStorage(String xmlFileName){
        final String fileName = xmlFileName;
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    InputStream stream = context.getApplicationContext().openFileInput(fileName);

                    xmlFactoryObject = XmlPullParserFactory.newInstance();
                    XmlPullParser myparser = xmlFactoryObject.newPullParser();
                    myparser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES
                            , false);
                    myparser.setInput(stream, null);
                    populateCurrencyList(myparser);
                    stream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();


    }

    public void insertRatesToDB(){

        // создаем объект для данных
        ContentValues cv = new ContentValues();

        // подключаемся к БД
        Log.d(TAG, "--- try dbHelper.getWritableDatabase ");
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Log.d(TAG, "--- Insert into table: ---");
                // подготовим данные для вставки в виде пар: наименование столбца - значение

        for(Cube cube:cubeList) {
            cv.put("currency", cube.get(Cube.CURRENCY));
            cv.put("rate", cube.get(Cube.RATE));
            // вставляем запись и получаем ее ID
            long rowID = db.insert(tableName, null, cv);
            Log.d(TAG, "row inserted, ID = " + rowID);
        }
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
            db.execSQL("create table"+ HandleXML.tableName +"("
                    + "id integer primary key autoincrement,"
                    + "currency text,"
                    + "rate text" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }


}
