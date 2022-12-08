/*
 Vanessa Lago Machado

this version the temporal time is considering only the time during a DAY, 
the same hours of the different days are considered equals
 */
package br.ufsc.methods;

import br.ufsc.model.SemanticAspect;
import br.ufsc.model.AttributeValue;
import br.ufsc.model.Centroid;
import br.ufsc.model.MultipleAspectTrajectory;
import br.ufsc.model.Point;
import br.ufsc.model.STI;
import br.ufsc.model.SemanticType;
import br.ufsc.model.TemporalAspect;
import br.ufsc.model.Util;
import br.ufsc.util.CSVWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author vanes
 */
public class MATSG_bkp {

    // setting to execute method
    String SEPARATOR;
    String[] valuesNulls;
    // --------------------- AUX ----------------------
    private static List<int[]> trajectories;
    private static int rId;
    private static int cId;
    int ord;
    private static String auxTid;
//    private static SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//    private static DecimalFormat df = new DecimalFormat("###.######");

    // -- Load
    // For loading information from the dataset
    private static List<Point> points; //Points to be analysed
    private static List<SemanticAspect> attributes; //List of all diferent attributes found in the dataset
    private static List<Point> pointsInCell; //List of all diferent attributes found in the dataset
    private static Map<String, BitSet> spatialCellGrid; //Spatial grid array
    private static Map<String, List<Double>> sematicNumericFusionVal;  //Sum of each type of numerical attribute
    private static Map<String, Map<String, Integer>> sematicCategoricalSummarizationVal; //Sum of ocorrunces os each categorical attribute

    // ------------- to Spatial division -- Dataset file information
    private static String filename; //Filename of the dataset
    private static String directory;//Directory of the dataset
    private static String extension; //Extension of the filename

    // To create the Spatial division    
    private static double spatialThreshold; //Maximum possible size for a cell
    private static double cellSizeSpace; //Size of each cell
    private static int valueZ; //Determines how many times the average dispersion of points will the cell size measure

    // To model trajectory data
    private static MultipleAspectTrajectory trajectory; //Contain all points of a MAT
    private static List<MultipleAspectTrajectory> listTrajectories; //List of all MATs in the dataset
    private static MultipleAspectTrajectory representativeTrajectory; //Summarized MAT

    //aux representative MAT for ordenate
    private static List<Centroid> listRepPoint;

    // To create the Temporal summarization
    private List<Integer> listTimesInCell; //List of all time marks in a cell 

    // To create the Spatial summarization
    private double avgX, avgY;

    // --------------- to determine categoricals pre-defined values
    List<String> lstCategoricalsPD;

    // --- Define initial index value to semantic attributes
    private static int INDEX_SEMANTIC = 4;

    //V9 - parameters to MAT-SG
    private float threshold_rc; //To define relevant cells 
    private float threshold_rv; //To define relevant values in rank values, which values in rank are representative
    private float rc; //To define relevant values in rank values, which values in rank are representative

    //aux to know the cell of each rp
    private String presentCell;

    /**
     * Method to perform all methods in order to summarize input MATs into a
     * representative MAT.
     *
     * @param spatialT Spatial Treshhold
     * @param file name of file
     * @param ext extension of file
     * @throws IOException
     *
     */
    public void execute(String dir, String file, String ext, String[] lstCategoricalPD, String SEPARATOR, String[] valuesNULL, int numberSpatialDistance, float rc, float threshold_rv) throws IOException, ParseException {

        //initialization of attribute values (Global attributes according to local data)
        directory = dir;
        filename = file;
        extension = ext;
        this.SEPARATOR = SEPARATOR;
        this.valuesNulls = valuesNULL;
        this.valueZ = numberSpatialDistance;

        //initialization of aux attributes
        rId = 0;
        auxTid = "-1";
        cId = -1;

        //initialization of aux lists
        listTimesInCell = new ArrayList<Integer>();
        spatialCellGrid = new HashMap<String, BitSet>();
        sematicNumericFusionVal = new HashMap<String, List<Double>>();
        sematicCategoricalSummarizationVal = new HashMap<String, Map<String, Integer>>();
        points = new ArrayList<Point>();
        attributes = new ArrayList<SemanticAspect>();
        listTrajectories = new ArrayList<MultipleAspectTrajectory>();
        pointsInCell = new ArrayList<>();

        lstCategoricalsPD = Arrays.asList(lstCategoricalPD);

        //initialization of object of MAT as representative MAT
        representativeTrajectory = new MultipleAspectTrajectory("representative");
        //aux representative MAT for ordenate
        listRepPoint = new ArrayList<>();

        load(); // Load dataset follow data model representation

//        spatialThreshold = computeSpatialThreshold() * valueZ; // Calculates the spatial threshold according with the informed z
        spatialThreshold = computeSpatialThresholdOutliers()* valueZ; // Calculates the spatial threshold according with the informed z
//        System.out.println("Spatial tau: "+spatialThreshold);
        
        cellSizeSpace = spatialThreshold * 0.7071; // Calcultes size of the cells
        allocateAllPointsInCellSpace(); // Distributes all points in the spatial grid

        //Parameter for defining representativeness values and compute relevant cell
        this.threshold_rv = threshold_rv;
        //rc is defined as the minimun number of points ( calculated by the % of all points) that should have in each cell
        this.rc = rc;
        threshold_rc = rc > 0.0 ? (rc * points.size()) : 2; //If rc is greater than zero sets threshold according with number of points, else sets to 2

        findCentroid(); //Creates the representative trajectory

        computeCentroid();

//        System.out.println("-----\n\n RT: " + representativeTrajectory);
        writeRepresentativeTrajectory("..\\" + directory +"result\\" + filename + "[output] - z" + this.valueZ, ext);
//        System.out.println("dir: "+"..\\" + directory +"result\\"+ filename + "[output] - z" + this.valueZ);
    }

    /**
     * Reads the dataset file and creates the all the MATs
     *
     * @throws IOException
     */
    private void load() throws IOException, ParseException {

        java.io.Reader input = new FileReader(directory + filename + extension);
        BufferedReader reader = new BufferedReader(input);

        String datasetRow = reader.readLine();
        //To Get the header of dataset
        String[] datasetColumns = datasetRow.split(SEPARATOR);

        //To add all types of attributes in the dataset, specified in the first line
        int order = 0;
        for (String s : Arrays.copyOfRange(datasetColumns, INDEX_SEMANTIC, datasetColumns.length)) {
            if (lstCategoricalsPD.contains(s.toUpperCase())) //If attribute was predefined as categorical
            {
                attributes.add(new SemanticAspect(s, order++, SemanticType.CATEGORICAL));
            } else {
                attributes.add(new SemanticAspect(s, order++));
            }

        }

        datasetRow = reader.readLine();

        //EoF - To get the trajectory data of dataset of each line
        while (datasetRow != null) {
            datasetColumns = datasetRow.split(SEPARATOR);
            addAttributeValues(datasetColumns);
            datasetRow = reader.readLine();
        }

        reader.close();

    }

    /**
     *
     * @param attrValues
     * @throws ParseException
     */
    private void addAttributeValues(String[] attrValues) throws ParseException {

        ++rId; //Id given to each data point 

        //Defines the semantic dimension as all attributes in predefined index to the end of line
        String[] semantics = Arrays.copyOfRange(attrValues, INDEX_SEMANTIC, attrValues.length);

        //All trajectory point follow the pattern:
        //id trajectory, coordinates (lat long), time, all semantic dimensions...
        // Follow the pattern add each MAT point in relative MAT
        addTrajectoryData(attrValues[0], attrValues[1].split(" "), formatDate.parse(attrValues[2]), semantics);
//        addTrajectoryData(attrValues[0], attrValues[1].split(" "), Util.convertMinutesToDate(Integer.parseInt(attrValues[2])), semantics);

    }

    /**
     * Add each MAT point in relative MAT object -- mapping input data to the
     * model predefined following O.O.
     *
     * @param tId - Id of MAT
     * @param coordinates - coordinates of point
     * @param time - time date of point
     * @param semantics - semantics attributes of point
     */
    private void addTrajectoryData(String tId, String[] coordinates, Date time, String[] semantics) {

        if (!tId.equals(auxTid)) { //IF the MAT is not created
            auxTid = tId;
            listTrajectories.add(new MultipleAspectTrajectory(Integer.valueOf(tId))); //Adds (Create) the new trajectory
            trajectory = listTrajectories.get(listTrajectories.size() - 1);
        }

        // aux values
        ArrayList<AttributeValue> attrs = new ArrayList<>();
        ord = 0;
        SemanticAspect a;

        //Organizes the point semantic attributes
        for (String val : semantics) {
            a = findAttributeForOrder(ord++);
            if (a.getType() != null && a.getType().equals(SemanticType.CATEGORICAL)) { //if it is predefined as Categorical
                val = "*" + val; // Use character '*' to force the number value to be a categorical value
            }
            if (Arrays.asList(valuesNulls).contains(val)) { // Define as Unknown null values
                val = "Unknown";
            }
            attrs.add(new AttributeValue(val, a));
        }
        a = null; //clean memory 

        //Adds the MAT point to current MAT
        trajectory.addPoint(new Point(rId,// para mexer no id do ponto
                Double.parseDouble(coordinates[0]),
                Double.parseDouble(coordinates[1]),
                time,
                attrs));

        //Adds current MAT point to list of points
        points.add(trajectory.getLastPoint());

    }

    /**
     * allocate all points of input dataset in spatial cell grid
     */
    public void allocateAllPointsInCellSpace() {
        for (Point p : points) {
            allocateInSpaceCell(p);
        }
    }

    /**
     * add each point in the relative grid cell
     *
     * @param coordinates Coordinates of each trajectory point
     *
     */
    private static void allocateInSpaceCell(Point p) {

        //Get x,y of the point in the spatial grid
        String key = getCellPosition(p.getX(), p.getY());

        //Get id of the spatial grid cell
        BitSet rIds = spatialCellGrid.get(key);

        //If the cell doesn't exist
        if (rIds == null) {
            //Creates the cell and adds to the spatial grid
            rIds = new BitSet();
            rIds.set(p.getrId());
            spatialCellGrid.put(key, rIds);
        } else {
            rIds.set(p.getrId());
            spatialCellGrid.replace(key, rIds);
        }
    }

    /**
     * Compute the cell position based on x and y divided by the cell size
     * predefined (cellSizeSpace)
     *
     * @param x
     * @param y
     * @return Cell Position
     */
    private static String getCellPosition(double x, double y) {

        return ((int) Math.floor(x / cellSizeSpace)) + "," + ((int) Math.floor(y / cellSizeSpace));

    }

    /**
     * Compute the representative point of each cell in the spatial grid,
     * summarizating all aspects
     */
    public void findCentroid() {


        //Create iterator object of all spatial grid cells
        Iterator<String> cell = spatialCellGrid.keySet().iterator();
//        System.out.println("tau points: "+threshold_rc);
        while (cell.hasNext()) {
            String cellAnalyzed = cell.next(); //Selects next cell
            
            //Gets amount of points in the current cell
            int qntPoints = spatialCellGrid.get(cellAnalyzed).cardinality();
//            System.out.println("Cell Analyzed: "+cellAnalyzed+" | points: "+qntPoints);
            if (qntPoints >= threshold_rc) { // IF number is at least a threshold RC
                resetValuesToSummarization();

                // Loop in all points of the cell
                for (int pointId = spatialCellGrid.get(cellAnalyzed).nextSetBit(0);
                        pointId >= 0;
                        pointId = spatialCellGrid.get(cellAnalyzed).nextSetBit(pointId + 1)) {
                    pointsInCell.add(points.get(pointId - 1));

                    //Temporal data 
                    listTimesInCell.add(points.get(pointId - 1).getTimeInMinutes()); // add time (in minutes) of point in a list 
                }
                presentCell = cellAnalyzed;
                //Temporal data
                defineRepPoints(listTimesInCell);

            }
        }
    }

    public void computeCentroid() {

        // Ordernate temporal ranking 
        listRepPoint = listRepPoint.stream().sorted().collect(Collectors.toList());

//        System.out.println("Lista RP: " + listRepPoint);
        for (Centroid representativePoint : listRepPoint) {
            resetValuesToSummarization();
            representativeTrajectory.addPoint(representativePoint);
            
            for (Point p : representativePoint.getPointListSource()) {
                // Spatial data
                avgX += p.getX();
                avgY += p.getY();

                //Semantic Data
                Double val;
                String attrActual;

                for (AttributeValue atv : p.getListAttrValues()) {
                    attrActual = "" + atv.getAttibute().getOrder();

                    // numeric values - median computation 
                    //in this scope just create bitset with sum and count of values foreach quantitative attribute
                    try {

                        val = Double.parseDouble((String) atv.getValue()); // val -1 refers to empty value
                        if (!sematicNumericFusionVal.containsKey(attrActual)) {
                            sematicNumericFusionVal.put(attrActual, new ArrayList<Double>());
                        }

                        // add into this key the attribute value 
                        sematicNumericFusionVal.get(attrActual).add(val);

                    } catch (java.lang.NumberFormatException e) { //categorical values
                        /*
                            in this scope create the sematicCategoricalSummarizationVal with all possible values of each categorical attribute
                             and add its ids for after this step can computation the frequency of each one,
                             and identify the value more frequency of each qualitative attribute 
                         */

                        //IF not contains this key - attribute name
                        if (!sematicCategoricalSummarizationVal.containsKey(attrActual)) {
                            sematicCategoricalSummarizationVal.put(attrActual, new HashMap<String, Integer>());
                        }

                        // IF this attribute not contains this value
                        if (!sematicCategoricalSummarizationVal.get(attrActual).containsKey(atv.getValue())) {
                            sematicCategoricalSummarizationVal.get(attrActual).put((String) atv.getValue(), 1); //add this value to attribute and initialize the count
                        } else {
                            sematicCategoricalSummarizationVal.get(attrActual).replace((String) atv.getValue(), sematicCategoricalSummarizationVal.get(attrActual).get(atv.getValue()) + 1);
                        }

                    }
                } //end FOR of all semantic attributes
//

            }
            // Spatial data
            representativePoint.setSpatialDimension(avgX / representativePoint.getPointListSource().size(), avgY / representativePoint.getPointListSource().size());

            // ---- Semantic data
            //Loop for numeric attributes
            sematicNumericFusionVal.entrySet().forEach((entrada) -> {
                Double median;
                Collections.sort(entrada.getValue());
                //Calculates the median value for all numeric attributes of the representative point
                if (entrada.getValue().size() % 2 == 0) {
                    median = (entrada.getValue().get(entrada.getValue().size() / 2) + entrada.getValue().get(entrada.getValue().size() / 2 - 1)) / 2;
                } else {
                    median = entrada.getValue().get(entrada.getValue().size() / 2);
                }

                representativePoint.addAttrValue("" + median,
                        findAttributeForOrder(Integer.parseInt(entrada.getKey())));
            });

            //begin -------- Loop for a categorical attributes
            //To see the size of categorical attributes 
            //System.out.println("Size of categorical attr: "+sematicCategoricalSummarizationVal.size());
            for (Map.Entry<String, Map<String, Integer>> allCategorical : sematicCategoricalSummarizationVal.entrySet()) {

                Map<String, Integer> internalCategoricalList
                        = allCategorical.getValue().entrySet()
                                .stream()
                                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
                //Add mode value (tendency) of attribute to representative point
                representativePoint.addAttrValue(normalizeRankingValues(internalCategoricalList, representativePoint.getPointListSource().size(), 's'),
                        findAttributeForOrder(Integer.parseInt(allCategorical.getKey())));
            } // end ------------ Loop for a categorical attributes

            //Reset values to representative point computation
            resetValuesToSummarization();
        }

        }
    

    /**
     * find the SemanticAspect object by the order
     *
     * @param order
     * @return SemanticAspect
     */
    public SemanticAspect findAttributeForOrder(int order) {
        for (SemanticAspect attr : attributes) {
            if (attr.getOrder() == order) {
                return attr;
            }
        }
        return null;
    }

    /**
     * Reset all values of all attributes in MAT
     */
    public void resetValuesToSummarization() {
        //Data reset

        //spatial data
        avgX = 0;
        avgY = 0;

        // semantic data (multiple aspects)
        sematicNumericFusionVal.clear();
//                sematicFusionCount.clear();
        sematicCategoricalSummarizationVal.clear();
        presentCell = "";

        //temporal data
        listTimesInCell.clear();
        //valid points in cell (temporal analysis)
        pointsInCell.clear();
    }

    /**
     * Computes the summarization of temporal data
     *
     * @param times occurence time of all input points in the cell
     * @return Map -- All STI (Significant Temporal Interval) and the number of
     * points in relative interval
     */
    public void defineRepPoints(List<Integer> times) {

        List<STI> listSTI = new ArrayList<>();

        //order times
        Collections.sort(times);
        List<Integer> differences = new ArrayList<>(); //List of local time intervals

        //determine threshold by avg
        int threshold = 100; //the predefined threshold used when the time value is less than or equal to 2 occurrences

        //Begin -- Calculates the intervals of time
        float sumDifferences = 0, sumDifferencesSD = 0;
        for (int i = 1; i < times.size(); i++) {
            int auxDif = times.get(i) - times.get(i - 1);
            if(auxDif > 0){
                differences.add(auxDif);
                sumDifferences += differences.get(differences.size() - 1);
            }
        }
        
        //Average temporal differences 
        float avg = sumDifferences / differences.size();


        if (differences.size() > 1) { //IF has more than 2 occurrences
            //order temporal differences
            Collections.sort(differences);
            /*
            compute the valid interval to remove the outliers
             -- computation: valid interval median minus and plus (- / +) SD.
             */
            //1st - compute the median value of the difference values
            if (differences.size() > 2) {
                int med;
                if (differences.size() % 2 == 1) {
                    med = differences.get(Math.floorDiv(differences.size(), 2));
                } else {
                    med = (differences.get(differences.size() / 2 - 1) + differences.get((differences.size() / 2))) / 2;
                }

                //2nd - Compute the SD
                for (int i = 0; i < differences.size(); i++) {
                    sumDifferencesSD += Math.pow(((int)differences.get(i) - avg), 2);
                }
                float SD = sumDifferencesSD / differences.size();
                SD = (float) Math.sqrt(SD);

                //3rd - compute the valid interval (the value of median of temporal differences  minus and plus (- / +) SD)

                float lessValue = med - SD;
                float upperValue = med + SD;
                
                    //for removing outliers:
                //remove values temporal differences less and upper the valid interval defined
                for (int i = 0; i < differences.size(); i++) {
                    if(differences.get(i) < lessValue){
                        sumDifferences -= (int)differences.get(i);
                        differences.remove((Integer)differences.get(i));
                        i--;
                    } else {
                        break;
                    }
                }
                
                for (int i = differences.size() - 1; i >= 0; i--) {
                    if(differences.get(i) > upperValue){
                        sumDifferences -= (int)differences.get(i);
                        differences.remove((Integer)differences.get(i));
                    } else {
                        break;
                    }
                }
                
//                System.out.println("Dif 2: "+differences);
                // update threshold value to average value of temporal differences considering only valid values
                threshold = Math.floorDiv((int) sumDifferences, differences.size());
            }
        }
        //End computation of temporal threshold

        int cont = 1;
//        Map<String, Integer> temporalRanking = new HashMap<>();
        STI newSTI;
        TemporalAspect aspTime = null;
//        System.out.println("TAU Temp: "+threshold);
        for (int i = 0; i < times.size(); i++) {

            /*
            IF the occurrence is not the last, 
            and two consecutive occurrences are considered a significant temporal interval (STI), 
            considering the threshold value, then it is considered a new valid interval
             */
            
            if (i != times.size() - 1 && (times.get(i) + threshold) >= times.get(i + 1)) {
                try {
                    aspTime.getStartTime();
                } catch (NullPointerException e) {
                    aspTime = new TemporalAspect(times.get(i)); // if not exist the object, it is created
                }
                cont++;
                /*
                IF has only one occurrence, this is add in the rank list or 
                if the occurence not is more considered into a previous STI 
                 */
            } else {

                //try get start time value -- else not has the instance of object aspTime created, the NullPointer is apointed
                try {
                    aspTime.getStartTime();
                    aspTime.setEndTime(times.get(i)); // if exist the object, the end time value is setted

                } catch (NullPointerException e) {
                    aspTime = new TemporalAspect(times.get(i)); // if not exist the object, it is created
                }

                listSTI.add(new STI(aspTime, (float) cont / times.size()));//add occurrence or STI into rank list
//                System.out.println("STI: "+aspTime+" points: "+cont+" times: "+times.size());
                //reset aux values
                cont = 1;
                aspTime = null;
            }
            
        }

        // Ordernate temporal ranking 
        listSTI.sort(Comparator.comparing(STI::getProportion).reversed()); // order (DESC) STI by proportion


        // based into valid interval, it is created the Representative points for further compute them
        for (STI eachSTI : listSTI) {
            
//            System.out.println("RV: "+threshold_rv);
//            System.out.println("STI: "+eachSTI);
//            System.out.println("prop: "+eachSTI.getProportion());
            if (eachSTI.getProportion() > threshold_rv) {

                Centroid repP = new Centroid();
                for (Point p : pointsInCell) {

                    if (eachSTI.getInterval().isInInterval(p.getTime().getStartTime())) {
                        //                System.out.println("Entrou");
                        repP.addPoint(p);
                    }
                }
                repP.setCellReference(presentCell);
//                System.out.println("repP: "+repP.getPointListSource());
//                if (!repP.getPointListSource().isEmpty() && repP.getPointListSource().size() >= threshold_rc) {

                if (!repP.getPointListSource().isEmpty()) {
                    repP.setSti(eachSTI);
                    listRepPoint.add(repP);
//                    System.out.println("RT: "+listRepPoint);
                    //representativeTrajectory.addPoint(repP);
                }
            }
        } //end loop in listSTI

    }

    /**
     * Compute the average of the minimum spatial distance of the input MATs
     * points to provide dynamic space segmentation for clustering these input
     * points. Given set of input MATs, with n points, we compute the Euclidean
     * distance d() for each point pi ∈ T with the nearest point pk ∈ T.
     *
     * @return spatialThreshold -- average of the minimum spatial distance
     */
    public double computeSpatialThresholdOutliers() {

        float minDistance = 999999999999999999L;
        float localDistance;
        float sumDistance = 0;
        ArrayList<Float> listMinDistances = new ArrayList<>();
//        double avgDistance;
        for (Point p : points) {
            for (Point q : points) {
                if (!p.equals(q)) {
                    localDistance = (float) Util.euclideanDistance(p, q);
                    if (localDistance < minDistance) {
                        minDistance = localDistance;
                    }
                }
            }
            sumDistance += minDistance;
            listMinDistances.add(minDistance);
            minDistance = 999999999999999999L;
            localDistance = 0;

        }

        float avgMinDist = (sumDistance / points.size());
        sumDistance = 0;

        // Remove outliers of minimun spatial distance
        if (listMinDistances.size() > 1) { //IF has more than 2 occurrences
            //order minimun spatial distance
            Collections.sort(listMinDistances);
            //-- computation: valid interval median minus and plus (- / +) SD.

            //1st - compute the median value of the difference values
            if (listMinDistances.size() > 2) {
                float medianMinDist;
                if (listMinDistances.size() % 2 == 1) {
                    medianMinDist = listMinDistances.get(Math.floorDiv(listMinDistances.size(), 2));
                } else {
                    medianMinDist = (listMinDistances.get(listMinDistances.size() / 2 - 1) + listMinDistances.get((listMinDistances.size() / 2))) / 2;
                }

                //2nd - Compute the SD
                for (int i = 0; i < listMinDistances.size(); i++) {
                    sumDistance += Math.pow((listMinDistances.get(i) - avgMinDist), 2);
                }
                float sdMinDist = sumDistance / listMinDistances.size();
                sdMinDist = (float) Math.sqrt(sdMinDist);

                //3rd - compute the valid interval (the value of median of minimum distance minus and plus (- / +) SD)
                float lessValueMinDist = medianMinDist - 4* sdMinDist;
                float upperValueMinDist = medianMinDist + 4* sdMinDist;

                //for removing outliers:
                sumDistance = 0;
                //remove values temporal differences less and upper the valid interval defined
                for (int i = 0; i < listMinDistances.size(); i++) {
                    if (listMinDistances.get(i) < lessValueMinDist || listMinDistances.get(i) > upperValueMinDist) {
                        listMinDistances.remove(i);
                    } else {
                        sumDistance += listMinDistances.get(i);
                    }
                }
            }
            return (sumDistance / listMinDistances.size());
        } 
            return listMinDistances.get(0);

        }
        
    
    public double computeSpatialThreshold() {

        double minDistance = 999999999999999999L;
        double localDistance;
        double sumDistance = 0;
        double avgDistance;
        for (Point p : points) {
            for (Point q : points) {
                if (!p.equals(q)) {
                    localDistance = Util.euclideanDistance(p, q);
                    if (localDistance < minDistance) {
                        minDistance = localDistance;
                    }
                }
            }
            sumDistance += minDistance;
            minDistance = 999999999999999999L;
            localDistance = 0;

        }
        //Returns the average of minimun distance beteween all points
        return (sumDistance / points.size());

    }
    
    
    
    /**
         * Writes the generated representative trajectory in a new .csv file
         *
         * @param fileOutput -- output file name
         * @param ext -- Extension of the file (e.g. csv)
         */
    public void writeRepresentativeTrajectory(String fileOutput, String ext) {
        try {
//            System.out.println(fileOutput + ext);
//            System.out.println("...");
            CSVWriter mxWriter = new CSVWriter("datasets/" + fileOutput + ext);

            for (Point p : representativeTrajectory.getPointList()) {
                mxWriter.writeLine(p.toString());
                mxWriter.flush();
            }

            mxWriter.writeLine("#");
            mxWriter.writeLine("RT setting infos:");
            mxWriter.writeLine("|input.T|, CellSize, tauRelevantCell, minPointsRC, tauRepresentativenessValue");
            System.out.println("|T| = "+listTrajectories.size());
//            for(MultipleAspectTrajectory mT: listTrajectories){
//                System.out.println(mT);
//            }
            mxWriter.writeLine(points.size() + ", " + cellSizeSpace + ", " + rc + ", " + threshold_rc + ", " + threshold_rv);
            mxWriter.writeLine("##");
            mxWriter.writeLine("RT infos:");
            mxWriter.writeLine("|rt|");
            mxWriter.writeLine(representativeTrajectory.getPointList().size());
            
            mxWriter.flush();

            mxWriter.close();
        } catch (IOException e) {
//					Logger.log(Type.ERROR, pfx + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * For updating the number of occurrences of each rank value by the ratio
     * value. Normalizing the Rank Value Map in the semantic or temporal
     * dimension, where the quantity of occurrences of each attribute value is
     * changed by the ratio of this value in relation to the size of the cell.
     *
     * In temporal dimension the minutes values are converted to valid time
     * information.
     *
     * @param mapRank -- currently the Map of ranking values with number of
     * occurrences for each value
     * @param sizeCell -- size of points in the cell
     * @param dimension -- t: temporal and s: semantic
     * @return normalized -- the Map update with ratio values of occurrences.
     */
    public Map<Object, Double> normalizeRankingValues(Map<String, Integer> mapRank, int sizeCell, char dimension) {

        Map<Object, Double> newMap = new HashMap<>();
        double trendEachVal;
        for (Map.Entry<String, Integer> eachValue : mapRank.entrySet()) {
            trendEachVal = (double) eachValue.getValue() / sizeCell;
            if (trendEachVal >= threshold_rv) {
                newMap.put(eachValue.getKey(), trendEachVal);
            }
        }

        Map<Object, Double> newMapSorted = newMap.entrySet().stream()
                .sorted(Map.Entry.<Object, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        if (dimension == 't') { // temporal dimension
            /*
            In the temporal dimension, the minutes values are converted to valid time according to the predefined mask.
             */
            Map<Object, Double> newTimeMap = new HashMap<>();
            for (Map.Entry<Object, Double> eachInt : newMapSorted.entrySet()) {
                String interval = (String) eachInt.getKey();
                String auxInterval;
                if (interval.contains("-")) {
                    auxInterval = formatDate.format(Util.convertMinutesToDate(Integer.parseInt(interval.substring(0, interval.indexOf("-")))));
                    auxInterval += " - " + formatDate.format(Util.convertMinutesToDate(Integer.parseInt(interval.substring(interval.indexOf("-") + 1))));
                } else {
                    auxInterval = formatDate.format(Util.convertMinutesToDate(Integer.parseInt(interval)));
                }

                newTimeMap.put(auxInterval, newMap.get(interval));
            }
            return newTimeMap;
        } else { // Semantic dimension
            return newMapSorted;
        }

    }

}
