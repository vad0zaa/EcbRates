package ee.sinchukov.ecbrates;

/**
 * Created by vsinchuk on 5/15/2015.
 */


import android.content.Context;
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

/**
 * Created by andreyutkin on 14/05/15.
 */
public class HandleXML  {
    Context fileContext;
    private String urlString = null;
    public volatile boolean parsingComplete = true;
    public volatile boolean getXmlNonComplete = true;
    private XmlPullParserFactory xmlFactoryObject;
    private ArrayList<String> currenciesList = new ArrayList<String>();
    private ArrayList<String> ratesList = new ArrayList<String>();

    ArrayList<Cube> cubeList = new ArrayList<>();
    private String receivedXmlData = "string builded from parsed XML";
    private String receivedXmlDate = "date and time from parsed XML";
    private static final String TAG = "HandleXML";

    public HandleXML(String url,Context fileContext){
        super();
        this.urlString = url;
        this.fileContext = fileContext;
    }

    public String getReceivedXmlData(){
        return receivedXmlData;
    }

    public String getReceivedXmlDate(){
        return receivedXmlDate;
    }
    public ArrayList<Cube> getCubeList(){
        return cubeList;
    }

    public ArrayList<String> getCurrenciesList(){
        return currenciesList;
    }

    public ArrayList<String> getRatesList(){
        return ratesList;
    }

    public void populateCurrencyList(XmlPullParser ecbParser) {

        try {

            int eventType = ecbParser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType==XmlPullParser.START_TAG && ecbParser.getName().equals("Cube") &&
                        ecbParser.getAttributeCount()==1) {
                    // date from parsed XML
                    receivedXmlDate =ecbParser.getAttributeValue(0);
                }

                if (eventType==XmlPullParser.START_TAG && ecbParser.getName().equals("Cube") &&
                        ecbParser.getAttributeCount()==2) {
                    // create Cube object and save into array
                    cubeList.add(new Cube(ecbParser.getAttributeValue(0),ecbParser.getAttributeValue(1)));
                }

                eventType = ecbParser.next();
                eventType = ecbParser.next();
            }
            parsingComplete = false;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fetchXMLfromURL(){
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
                            receivedXmlData = stringBuilder.toString();
                            getXmlNonComplete = false;
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

    public void fetchXMLfromInternalStorage(String xmlFileName){
        final String fileName = xmlFileName;
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    InputStream stream = fileContext.getApplicationContext().openFileInput(fileName);

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
}
