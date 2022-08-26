/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.tests;




import br.ufsc.methods.MATSG;
import static br.ufsc.tests.TestTrajectoryCellSize.dir;
import static br.ufsc.tests.TestTrajectoryCellSize.extension;
import static br.ufsc.tests.TestTrajectoryCellSize.filename;
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

        dir = "datasets\\";
        filename = "Running_Example_v2";
        extension = ".csv";


        MATSG method = new MATSG();
        
        //informando lista de att a ser forçados como categoricos, mesmo contendo números
        String[] lstCategoricalsPreDefined = {"PRICE"};
        //
         String[] lstToIgnore = {"label".toUpperCase()};

        String SEPARATOR = ",";
        String[] valuesNulls = {"Unknown", "*-1"};
        
        int thresholdCellSize = 6;
//        method.execute(dir,filename, extension, lstCategoricalsPreDefined, SEPARATOR, valuesNulls,thresholdCellSize);
        float rc = 0.1f;
        float threshold_rv = 0.25f;

        method.execute(dir, filename, extension, lstCategoricalsPreDefined, SEPARATOR, valuesNulls, thresholdCellSize, rc, threshold_rv);
    }
}
