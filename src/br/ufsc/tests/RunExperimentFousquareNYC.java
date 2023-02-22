/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.tests;




import br.ufsc.methods.MATSG_R;
import java.io.IOException;
import java.text.ParseException;

/**
 *
 * @author vanes
 */
public class RunExperimentFousquareNYC {

    public static String filename;
    public static String extension;
    public static String dir;

    public static void main(String[] args) throws IOException, ParseException, CloneNotSupportedException {

        dir = "datasets\\Foursquare-nyc\\";
//        filename = "Running_Example_v5";
        filename = args[0];
        extension = ".csv";

        
        //informando lista de att a ser forçados como categoricos, mesmo contendo números
        String[] lstCategoricalsPreDefined = {"poi","price"};
        for (int i = 0; i < lstCategoricalsPreDefined.length; i++) {
            lstCategoricalsPreDefined[i] = lstCategoricalsPreDefined[i].toUpperCase();
        }
        
        String SEPARATOR = ",";
        
        String[] valuesNulls = {"Unknown", "*-1", "*-999", "", "*"};

        
        String[] lstIgnoreColumns = {"label"};
        for (int i = 0; i < lstIgnoreColumns.length; i++) {
            lstIgnoreColumns[i] = lstIgnoreColumns[i].toUpperCase();
        }

        
//        float rc = 0.1f;
        float rc = Float.parseFloat(args[2]);
        float threshold_rv = Float.parseFloat(args[3]);
//        float threshold_rv = 0.0f;
//        String patternDate = "yyyy-MM-dd HH:mm:SS.SSS";
        String patternDateIn = "?"; //For minutes time (integer value) inform '?' character
        
//        String patternDateOut = "HH:mm:ss";

//        method.execute(dir, filename, extension, lstCategoricalsPreDefined, SEPARATOR, valuesNulls, patternDateIn, rc, threshold_rv);

        if (args[1].equals("R")) {
            //method R
            MATSG_R method = new MATSG_R();
//            method.notConsiderNulls();
            method.execute(dir, filename, extension, lstCategoricalsPreDefined, SEPARATOR, valuesNulls, lstIgnoreColumns, patternDateIn, rc, threshold_rv);
        }else {
            System.err.println("Argumento não encontrado: "+args[1]);
        }
        
        
    }

}
