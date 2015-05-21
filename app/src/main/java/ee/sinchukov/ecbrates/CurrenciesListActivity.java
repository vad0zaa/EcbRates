package ee.sinchukov.ecbrates;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import ee.sinchukov.ecbrates.HandleXML;


public class CurrenciesListActivity extends ListActivity {
    private static final String TAG = "MainActivity";
    private static final String FILENAME = "saved_Ecb_Rates.xml";
    private static final String SETTINGS_NAME = "ecbrates_settings";
    private static final String PREFS_XML_DATE = "parsed_xml_date";
    ArrayList<Cube> cubeList = new ArrayList<>();

    ArrayList<String> currenciesList = new ArrayList<String>();
    HandleXML handleXML;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleXML=new HandleXML("http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml");
        handleXML.fetchXML();
        while(handleXML.parsingComplete);
        currenciesList=handleXML.getRatesList();
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, currenciesList));

        // SimpleAdapter
        String[] from=new String[] { Cube.CURRENCY, Cube.RATE };
        int[] to=new int[] {R.id.currencyView, R.id.rateView };

        cubeList = handleXML.getCubeList();
        ListAdapter adapter = new SimpleAdapter(this, cubeList, R.layout.activity_currencies_list,from,to);
        setListAdapter(adapter);

        //save xml file to internal storage
        writeToFile(handleXML.getReceivedXmlData(),FILENAME);

        //save parsed xml file date into preferences
        saveDataToApplicationSettings(PREFS_XML_DATE,handleXML.getReceivedXmlDate());
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
    
    private String readFromFile(String fileName) {
        String fileContent = "";
        try {
            InputStream inputStream = openFileInput(fileName);
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                fileContent = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }
        return fileContent;
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

}
