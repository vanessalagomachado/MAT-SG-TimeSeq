/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package measure;


import br.ufsc.model.AttributeValue;
import br.ufsc.model.Centroid;
import br.ufsc.model.MultipleAspectTrajectory;
import br.ufsc.model.Point;
import br.ufsc.model.STI;
import br.ufsc.model.SemanticAspect;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author vanes
 */
public class MUITAS {

    private Map<Object, Float> weights;
    private Map<SemanticAspect, Float> thresholds;
//    private Map<Attribute, DistanceFunction<Attribute>> distanceFunctions;

    private double parityT1T2 = 0;
    private double parityT2T1 = 0;

    public MUITAS() {
        this.weights = new HashMap<Object, Float>();
        this.thresholds = new HashMap<SemanticAspect, Float>();
    }
    


        public void clear() {
        this.weights.clear();
        this.thresholds.clear();
    }

    public void setThreshold(SemanticAspect att, float threshold) {
        this.thresholds.put(att, threshold);
//        System.out.println("Lista de thresholds");
    }

    public double getThreshold(SemanticAspect att) {
        if (thresholds.isEmpty()) {
            System.err.println("threshold list is Empty");
        }

        try {
            return this.thresholds.get(att);
        } catch (Exception e) {
            System.err.println("Error in getThreshold for attribute: '" + att.getName() + "' (thresholds: " + this.thresholds + ")");
            throw new NullPointerException();
        }
    }
    public void setWeight(Object attribute, float weight) {
            this.weights.put(attribute, weight);
    }

    public double getWeight(Object attribute) {
        try {
            if(attribute instanceof STI){
//                System.err.println("Deu certo o weight para Time: "+this.weights.get("TIME"));
                return this.weights.get("TIME");
            }else{
//                System.err.println("weight of: "+attribute+" = "+this.weights.get(attribute));
                return this.weights.get(attribute);
            }
        } catch (Exception e) {
            System.err.println("Error in getWeight for feature: '" + attribute + "' (weights: " + this.weights + ")");
            throw new NullPointerException();
        }
    }
    
    public float getAllWeight() {
        float sumWeight = 0.0f;
        

        for (Map.Entry<Object, Float> eachWeight : weights.entrySet()) {
            sumWeight += eachWeight.getValue();
            
        }
        return sumWeight;
    }


    public double getParityT1T2() {
        return parityT1T2;
    }

    public double getParityT2T1() {
        return parityT2T1;
    }

    public double similarityOf(MultipleAspectTrajectory t1, MultipleAspectTrajectory t2) throws ParseException {
        parityT1T2 = 0;
        parityT2T1 = 0;
        double[][] scores = new double[t1.getPointList().size()][t2.getPointList().size()];

        for (int i = 0; i < t1.getPointList().size(); i++) {
            double maxScoreRow = 0;

            for (int j = 0; j < t2.getPointList().size(); j++) {
//                System.out.println("Pontos a serem analisados: "+t1.getPointList().get(i)+"\nand: "+t2.getPointList().get(j));
                scores[i][j] = this.score((Centroid) t1.getPointList().get(i), t2.getPointList().get(j));
//                System.out.println("Score [" + i + ", " + j + "] = " + scores[i][j]);
                maxScoreRow = scores[i][j] > maxScoreRow ? scores[i][j] : maxScoreRow;
            }

//            System.out.println("----- x ------");
            parityT1T2 += maxScoreRow;

        }
        for (int j = 0; j < t2.getPointList().size(); j++) {
            double maxCol = 0;

            for (int i = 0; i < t1.getPointList().size(); i++) {

                maxCol = scores[i][j] > maxCol ? scores[i][j] : maxCol;
            }

            parityT2T1 += maxCol;
        }
        return (parityT1T2 + parityT2T1) / (t1.getPointList().size() + t2.getPointList().size());

    }

    private final double score(Centroid p1, Point p2) throws ParseException {
        double score = 0;
        if (p1.getPointListSource().contains(p2)) {
             score += (getWeight("SPATIAL"));
//            System.out.println("Spatial Match: "+score);
        }

        matchTemporal:
        {
            double match = 0;
            
            //Calendar timeStart = Calendar.getInstance(), timeEnd = Calendar.getInstance();

            if (p1.getSti().getInterval().isInInterval(p2.getTime().getStartTime())){
                match = p1.getSti().getProportion();
//                System.out.println("Match Temporal");
            }

            // Vanessa: aqui criar uma classe maior com os tipos TemporalAspect e SemanticAspect, para poder vincular o peso
            score += match * getWeight(p1.getSti());
            
            
        }

        for (AttributeValue atvP1 : p1.getListAttrValues()) {
            AttributeValue tempAttP2 = atvP1.getAttibute() != null ? p2.getAttributeValue(atvP1.getAttibute()) : null;
            double tempSemanticMatch = computeMatch(atvP1, tempAttP2);
//            System.out.println("Match semantic: "+tempSemanticMatch);
            score += tempSemanticMatch;

        }
        return score;
    }

    public double computeMatch(AttributeValue rep, AttributeValue atv) {
        double match = 0;
//        System.out.println("Att RP: "+rep +"  x  att T2: "+atv);
        if(atv == null || rep == null)
            return 0;

//                System.out.println("Value of attr value on compute match: "+rep.getValue());
                if (rep.getValue() instanceof Map) { // Categorical values

                // case of semantic - categorical
                HashMap<String, Double> valuesRT = (HashMap) rep.getValue();
                if (valuesRT.containsKey(((String) atv.getValue()).toUpperCase())) {
                    match = valuesRT.get(((String) atv.getValue()).toUpperCase());
                }
            } else {
                
                if(rep.getNumericalValueSD() != 0)
                    match = Math.abs(Double.parseDouble((String)rep.getValue()) - Double.parseDouble((String)atv.getValue())) <= (rep.getNumericalValueSD() * 2.5) ? 1.0 : 0;
                else //default value
                    match = Math.abs(Double.parseDouble((String)rep.getValue()) - Double.parseDouble((String)atv.getValue())) <= 10 ? 1.0 : 0;
//                
//                System.out.println("Numerical value: "+atv.getValue()+" -- RT value: "+rep.getValue()+" --Threshold value:  "+rep.getNumericalValueSD()*2.5+" -- Match value: "+match);
            }

        return match * getWeight(rep.getAttibute());
    }

    // Colocar em Util se der certo
    public AttributeValue findAttributeValue(String name, List<AttributeValue> list) {
        for (AttributeValue attr : list) {
            if (attr.getAttibute().getName().equalsIgnoreCase(name)) {
                return attr;
            }
        }
        return null;
    }

}
