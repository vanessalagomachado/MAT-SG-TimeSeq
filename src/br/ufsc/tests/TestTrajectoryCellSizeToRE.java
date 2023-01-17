/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.tests;




import br.ufsc.methods.MATSG;
import br.ufsc.methods.MATSG_RE;
import static br.ufsc.tests.RunExperimentHermopolis.dir;
import static br.ufsc.tests.RunExperimentHermopolis.extension;
import static br.ufsc.tests.RunExperimentHermopolis.filename;
import java.io.IOException;
import java.text.ParseException;

/**
 *
 * @author vanes
 */
public class TestTrajectoryCellSizeToRE {

    public static String filename;
    public static String extension;
    public static String dir;

    public static void main(String[] args) throws IOException, ParseException {

        dir = "datasets\\RE\\";
        filename = "Running_Example_v5";
        extension = ".csv";


        MATSG method = new MATSG();
        
        
        //informando lista de att a ser forçados como categoricos, mesmo contendo números
        String[] lstCategoricalsPreDefined = {"price"};
        for (int i = 0; i < lstCategoricalsPreDefined.length; i++) {
            lstCategoricalsPreDefined[i] = lstCategoricalsPreDefined[i].toUpperCase();
        }
        
        String SEPARATOR = ",";
        
        String[] valuesNulls = {"Unknown", "*-1", "*-999", "", "*"};

        float rc = 0.1f;
        float threshold_rv = 0.2f;
//        String patternDate = "yyyy-MM-dd HH:mm:SS.SSS";
        String patternDate = "?"; //For minutes time (integer value) inform '?' character
        

        method.execute(dir, filename, extension, lstCategoricalsPreDefined, SEPARATOR, valuesNulls, patternDate, rc, threshold_rv);
    }

}
