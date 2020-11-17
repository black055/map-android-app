package modules;

import java.util.ArrayList;

public interface CovidInterface {
    void getDataSuccessful(ArrayList<String> nameCountry,
    ArrayList<Integer> cases ,
    ArrayList<Integer> dead ,
    ArrayList<Integer> recovered,
    ArrayList<String> lat ,
    ArrayList<String> lng );
}
