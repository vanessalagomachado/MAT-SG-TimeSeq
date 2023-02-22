/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.tests;

import br.ufsc.methods.MATSG_R;
import static br.ufsc.tests.RunExperimentSanFrancisco.dir;
import static br.ufsc.tests.RunExperimentSanFrancisco.extension;
import static br.ufsc.tests.RunExperimentSanFrancisco.filename;
import java.io.IOException;
import java.text.ParseException;

/**
 *
 * @author vanes
 */
public class RunExperimentRE_Manual {

    public static String filename;
    public static String extension;
    public static String dir;

    public static void main(String[] args) throws IOException, ParseException, CloneNotSupportedException {

        dir = "datasets\\RE\\";
        filename = "Running_Example_v5";
//        filename = args[0];
        extension = ".csv";

        //informando lista de att a ser forçados como categoricos, mesmo contendo números
        String[] lstCategoricalsPreDefined = {"price"};
        for (int i = 0; i < lstCategoricalsPreDefined.length; i++) {
            lstCategoricalsPreDefined[i] = lstCategoricalsPreDefined[i].toUpperCase();
        }

        String SEPARATOR = ",";

        String[] valuesNulls = {"Unknown", "*-1", "*-999", "", "*"};

        float rc = 0.1f;
        float threshold_rv = 0.25f;
//        String patternDate = "yyyy-MM-dd HH:mm:SS.SSS";
        String patternDateIn = "?"; //For minutes time (integer value) inform '?' character
        String[] lstIgnoreColumns = null;
        //method R
        MATSG_R method = new MATSG_R();
//            method.notConsiderNulls();
        method.execute(dir, filename, extension, lstCategoricalsPreDefined, SEPARATOR, valuesNulls, lstIgnoreColumns, patternDateIn, rc, threshold_rv);

    }

}
