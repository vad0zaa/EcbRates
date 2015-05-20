package ee.sinchukov.ecbrates;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by user_39 on 20.05.2015.
 */
public class Cube extends HashMap<String, String> {

    public static final String CURRENCY = "currency";
    public static final String RATE = "rate";

    Cube(String currency, String rate){
        super();
        super.put(CURRENCY, currency);
        super.put(RATE, rate);

    }

}
