/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufsc.tests;

import br.ufsc.methods.MATSG;
import java.io.IOException;
import java.text.ParseException;

/**
 *
 * @author vanes
 */
public class TestTrajectoryCellSize {

    public static String filename;
    public static String extension;
    public static String dir;

    public static void main(String[] args) throws IOException, ParseException {

//        dir = "datasets\\Foursquare - user 6\\";
        dir = "datasets\\Experimento\\";
//        filename = "Foursquare - user 6 - v2";
        filename = "sanfrancisco_groundtruth_experimento[A2][formatted]";
        extension = ".csv";
        int z;
//        if(args.length > 101 ){            
        z = Integer.parseInt(args[0]);
//              z = 10;  
//        }

        MATSG method = new MATSG();

        //informando lista de att a ser forçados como categoricos, mesmo contendo números
//        String[] lstCategoricalsPreDefined = {"PRICE","rating".toUpperCase(),"label".toUpperCase(),"checkin_id".toUpperCase(),"venue_id".toUpperCase()};
        String[] lstCategoricalsPreDefined = {"gid", "taxi_id", "route", "semantic_stop_id", "semantic_move_id"};
        for (int i = 0; i < lstCategoricalsPreDefined.length; i++) {
            lstCategoricalsPreDefined[i] = lstCategoricalsPreDefined[i].toUpperCase();
        }
        //
//        String[] lstToIgnore = {"label".toUpperCase(), "time".toUpperCase()};

        String SEPARATOR = ",";
//        double thresholdCellSize = 10; 
        String[] valuesNulls = {"Unknown", "*-1", "*-999", "", "*"};

        float rc = 0.01f;
        float threshold_rv = 0.0f;

        method.execute(dir, filename, extension, lstCategoricalsPreDefined, SEPARATOR, valuesNulls, z, rc, threshold_rv);
    }
}
