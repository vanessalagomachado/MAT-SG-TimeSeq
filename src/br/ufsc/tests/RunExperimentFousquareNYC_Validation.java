/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.tests;




import br.ufsc.methods.MATSG_R;
import br.ufsc.methods.MATSG_R_Validation;
import br.ufsc.methods.MATSG_S_Validation;
import br.ufsc.methods.MATSG_T_Validation;
import br.ufsc.methods.MATSG_TimeSeq;
import br.ufsc.methods.MATSG_Validation;
import java.io.IOException;
import java.text.ParseException;

/**
 *
 * @author vanes
 */
public class RunExperimentFousquareNYC_Validation {

    public static String filename;
    public static String extension;
    public static String dir;

    public static void main(String[] args) throws IOException, ParseException, CloneNotSupportedException {

        dir = "datasets\\Foursquare-nyc\\";
//        filename = "Running_Example_v5";
        filename = args[0];
        extension = ".csv";

        
        //informando lista de att a ser forçados como categoricos, mesmo contendo números
        String[] lstCategoricalsPreDefined = {"price"};
        for (int i = 0; i < lstCategoricalsPreDefined.length; i++) {
            lstCategoricalsPreDefined[i] = lstCategoricalsPreDefined[i].toUpperCase();
        }
        
        String SEPARATOR = ",";
        
        String[] valuesNulls = {"Unknown", "*-1", "*-999", "", "*"};

        
        String[] lstIgnoreColumns = {"label","poi"};
        for (int i = 0; i < lstIgnoreColumns.length; i++) {
            lstIgnoreColumns[i] = lstIgnoreColumns[i].toUpperCase();
        }
        System.out.println(lstIgnoreColumns);
        
//        float rc = 0.1f;
        float rc = Float.parseFloat(args[2]);
        float threshold_rv = Float.parseFloat(args[3]);
//        float threshold_rv = 0.0f;
//        String patternDate = "yyyy-MM-dd HH:mm:SS.SSS";
        String patternDateIn = "?"; //For minutes time (integer value) inform '?' character
        
//        String patternDateOut = "HH:mm:ss";

//        method.execute(dir, filename, extension, lstCategoricalsPreDefined, SEPARATOR, valuesNulls, patternDateIn, rc, threshold_rv);

        if (args[1].equals("S")) {
            //method R
//            MATSG_S_Validation method = new MATSG_S_Validation();
                MATSG_TimeSeq method = new MATSG_TimeSeq();
//            method.notConsiderNulls();
            method.setFilenameFullDataset(args[4]);
            method.execute(dir, filename, extension, lstCategoricalsPreDefined, SEPARATOR, valuesNulls, lstIgnoreColumns, patternDateIn, rc, threshold_rv);
        } else         if (args[1].equals("T")) {
            //method R
//            MATSG_T_Validation method = new MATSG_T_Validation();
              MATSG_TimeSeq method = new MATSG_TimeSeq();
//            method.notConsiderNulls();
            method.setFilenameFullDataset(args[4]);
            method.execute(dir, filename, extension, lstCategoricalsPreDefined, SEPARATOR, valuesNulls, lstIgnoreColumns, patternDateIn, rc, threshold_rv);
        }
        else {
            System.err.println("Argumento não encontrado: "+args[1]);
        }
        
        
    }

}
