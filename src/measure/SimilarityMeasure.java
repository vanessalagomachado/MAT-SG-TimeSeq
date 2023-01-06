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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author vanes
 */
public class SimilarityMeasure {

    private Map<Object, Float> weights;
    private Map<SemanticAspect, Float> thresholds;
//    private String patternTemporalData;

    private double recallRT_T = 0;

    public SimilarityMeasure() {
        this.weights = new HashMap<Object, Float>();
        this.thresholds = new HashMap<SemanticAspect, Float>();
    }
    
//    public SimilarityMeasure(String formatTemporalData) {
//        this.weights = new HashMap<Object, Double>();
//        this.thresholds = new HashMap<SemanticAspect, Double>();
//        patternTemporalData = formatTemporalData;
//        
//    }
//    
//    public SimilarityMeasure(SimpleDateFormat formatTemporalData) {
//        this.weights = new HashMap<Object, Double>();
//        this.thresholds = new HashMap<SemanticAspect, Double>();
//        
//        
//    }

    public void clear() {
        this.weights.clear();
        this.thresholds.clear();
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

    public double getRecallRT_T() {
        return recallRT_T;
    }

    
    /**
     * Compute how much RT is capturing on the information contained in each original Trajectory (T). This value is given by parity between T and RT divided by the size of the T.
     * 
     * @param RT - representative trajectory 
     * @param t2 - an input trajectory
     * @return the recall value
     * @throws ParseException 
     */
    public double recallOf(MultipleAspectTrajectory RT, MultipleAspectTrajectory t2) throws ParseException {
        recallRT_T = 0;
        //Matrix RT x T
        double[][] scores = new double[RT.getPointList().size()][t2.getPointList().size()];

        //for each point of the input trajectory, analyse with all points of the RT
        for (int j = 0; j < t2.getPointList().size(); j++) {
            double maxCol = 0;

            for (int i = 0; i < RT.getPointList().size(); i++) {
                scores[i][j] = this.score((Centroid) RT.getPointList().get(i), t2.getPointList().get(j));
                maxCol = scores[i][j] > maxCol ? scores[i][j] : maxCol; //identify the point of the RT more similar (max score) with the actual point of input T.
            }

            recallRT_T += maxCol;
        }
        return (recallRT_T) / (t2.getPointList().size());

    }

    /**
     * Compute score value between two points - one of RT and one of T2
     * the score value is the sum of each aspect match in context. 
     * the aspect match value is considered following:
     *  - spatial aspect - when the point of T2 is into the same cell of RT centroid.
     *  - temporal aspect - when the temporal value of T2 is into some interval in RT or if it is equal an ponctual occurence.
     *  - semantic dimension - categorical aspects: when the value of T2 is into some interval of RT.
     *  - semantic dimension - numerical aspects: when the value of T2 is into the defined threshold of difference values with RT value.
     * 
     * @param p1 - Centroid point
     * @param p2 - a point of T2
     * @return score value
     * @throws ParseException 
     */
    private double score(Centroid p1, Point p2) throws ParseException {
        double score = 0;
        if (p1.getPointListSource().contains(p2)) {
            score += (getWeight("SPATIAL"));
//            System.out.println("Spatial Match: "+score);
//             System.out.println("Match spatial");
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

    /**
     * Compute the match value between two attribute values (RT and T2)
     * the match value is the final value of each attribute - match value * defined weight
     * 
     * @param rep
     * @param atv
     * @return the value of the match
     */
    public double computeMatch(AttributeValue rep, AttributeValue atv) {
        double match = 0;
//        System.out.println("Att RP: "+rep +"  x  att T2: "+atv);
        if(atv == null || rep == null)
            return 0;

        // compute temporal match 
//        if (!rep.getAttibute().getName().equalsIgnoreCase("TIME")
//                && rep.getAttibute().equals(atv.getAttibute())) {
//            if (rep.getValue() instanceof Number) {
//                //Case of Semantic - numeric
//                if (getThreshold(rep.getAttibute()) >= Math.abs(Double.valueOf(rep.getValue().toString()) - Double.valueOf(atv.getValue().toString()))) {
//                    match = 1;
//                }
//            } else 
                if (rep.getValue() instanceof Map) {
//                    System.out.println("Entrou MAP");
                // case of semantic - categorical
                HashMap<String, Double> valuesRT = (HashMap) rep.getValue();
//                    System.out.println(valuesRT);
//                    System.out.println("valor na T: "+((String) atv.getValue()).toUpperCase());
                if (valuesRT.containsKey(((String) atv.getValue()).toUpperCase())) {
//                    System.out.println("Achou a chave");
                    match = valuesRT.get(((String) atv.getValue()).toUpperCase());
                }
            }
//        }

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
