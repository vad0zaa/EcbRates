package ee.sinchukov.ecbrates;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

import ee.sinchukov.ecbrates.HandleXML;


public class CurrenciesListActivity extends ListActivity {

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
    }


}
