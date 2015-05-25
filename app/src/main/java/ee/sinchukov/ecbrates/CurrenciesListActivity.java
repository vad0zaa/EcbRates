package ee.sinchukov.ecbrates;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class CurrenciesListActivity extends ListActivity {
    private static final String TAG = "MainActivity";
    private static final String SETTINGS_NAME = "ecbrates_settings";
    private static final String PREFS_XML_DATE = "parsed_xml_date";

    private String xmlDateFromPrefs;
    private SimpleDateFormat ecbRateDateFormatter;

    private static final String FILENAME = "saved_Ecb_Rates.xml";
    private static final int ecbUpdatingTimeHour = 15;
    private static final int ecbUpdatePeriodHours = 24;
    private static final String ecbRatesTimezone = "CET";
    private static final String ecbRatesXMLDateFormat = "yyyy-MM-dd";
    private static final String ecbRatesDefaultDate = "1999-01-01";
    private  static final String ecbRateUrl = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

    private HandleXML handleXML;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ecbRateDateFormatter = new SimpleDateFormat(ecbRatesXMLDateFormat);
        ecbRateDateFormatter.setTimeZone(TimeZone.getTimeZone(ecbRatesTimezone));

        //date of last saved currency rates
        xmlDateFromPrefs = getDataFromApplicationSettings(PREFS_XML_DATE);
        if(xmlDateFromPrefs.equals("not found")){xmlDateFromPrefs=ecbRatesDefaultDate;}

        // prepare for XML parsing
        handleXML=new HandleXML(ecbRateUrl, this);

        // if rates are not fresh, then get new from URL and save xml file to internal storage
        if(!isFreshRates(xmlDateFromPrefs)) {
            handleXML.xmlFromUrlToString();
            while (handleXML.getXmlStringFromUrlNonComplete) ;
            writeToFile(handleXML.getXmlStringDataFromUrl(), FILENAME);
            saveDataToApplicationSettings(PREFS_XML_DATE,handleXML.getReceivedXmlDate());
        }

        // parse xml from internal storage
        handleXML.fetchXmlFromInternalStorage(FILENAME);
        while(handleXML.parsingNonComplete);

        // display rates on screen
        String[] from=new String[] { Cube.CURRENCY, Cube.RATE };
        int[] to=new int[] {R.id.currencyView, R.id.rateView };
        ListAdapter adapter = new SimpleAdapter(this, handleXML.getCubeList(), R.layout.activity_currencies_list,from,to);
        setListAdapter(adapter);

    }

    private boolean isFreshRates(String xmlDateFromPrefs){
        final int MILLI_TO_HOUR = 1000 * 60 * 60;
        Date savedDate=null;

        // parse date of local saved ecb rates xml file
        try {
            savedDate = ecbRateDateFormatter.parse(xmlDateFromPrefs);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // add time to day of last saved ecb rates
        Calendar c = Calendar.getInstance();
        c.setTime(savedDate);
        c.add(Calendar.HOUR, ecbUpdatingTimeHour);
        savedDate = c.getTime();

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
        int difference = (int) getCurrentDateCET().getTime() - nextUpdate;

        if(difference >0){
            // local saved rates are old,  need to update from URL
            return false;}
        else{
            // local saved rates are fresh, use xml from internal storage
            return true;}

    }

    public Date getCurrentDateCET(){

        Date dateCET = null;
        SimpleDateFormat currentDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        currentDF.setTimeZone(TimeZone.getTimeZone("CET"));
        String currentDate = currentDF.format(new Date());

        try {
            dateCET =  currentDF.parse(currentDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dateCET;
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

}
