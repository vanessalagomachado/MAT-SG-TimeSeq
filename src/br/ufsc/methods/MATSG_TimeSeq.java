/*
 */
package br.ufsc.methods;

import br.ufsc.model.AttributeValue;
import br.ufsc.model.Centroid;
import br.ufsc.model.MultipleAspectTrajectory;
import br.ufsc.model.Point;
import br.ufsc.model.STI;
import br.ufsc.model.SemanticAspect;
import br.ufsc.model.SemanticType;
import br.ufsc.model.TemporalAspect;
import br.ufsc.model.Util;
import br.ufsc.util.CSVWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import measure.MUITAS;
import measure.SimilarityMeasure;

/**
 *
 * @author Vanessa
 */
public class MATSG_TimeSeq {
    // Attributes 

    // ------------- to Spatial division -- Dataset file information
    private String filename; //Filename of the dataset
    private String directory;//Directory of the dataset
    private String extension; //Extension of the filename

    private String SEPARATOR;

    // --- Define initial index value to semantic attributes
    private int INDEX_SEMANTIC = 3;
    private boolean considerNulls = true;

    // --------------- to determine categoricals pre-defined values
    List<String> lstCategoricalsPD;
    List<String> lstIgnoreCols = null;
    String[] valuesNulls; //witch values are considered null in input dataset?

    //-- parameters to MAT-SG (defined by the user)
    private float threshold_rc; //To define relevant cells 
    private float threshold_rv; //To define relevant values in rank values, which values in rank are representative
    private float rc; //To define relevant values in rank values, which values in rank are representative

    // -- Load
    // For loading information from the dataset
    private static List<SemanticAspect> attributes; //List of all diferent attributes found in the dataset
    private static List<Point> points; //Points to be analysed
    private static Map<String, BitSet> spatialCellGrid; //Spatial grid array
    private static List<Point> pointsInCell; //List of all diferent points found in the each cell

    // format of input data
    private static SimpleDateFormat formatDate;
    // format of output data
    private static SimpleDateFormat formatDateOut;

// --------------------- AUX ----------------------
    private static int rId;
    private static String auxTid;

    // To model trajectory data
    private static MultipleAspectTrajectory trajectory; //Contain all points of a MAT
    private static List<MultipleAspectTrajectory> listTrajectories; //List of all MATs in the dataset
    private static MultipleAspectTrajectory betterRT; //Better Summarized MAT

    /// ----- Spatial Segmentation
    // To create the Spatial division    
    private static float spatialThreshold; //Maximum possible size for a cell
    private static double cellSizeSpace; //Size of each cell
    private float auxMaxZ; //

    /// ---- Summarization step
    // To create the Temporal summarization
    private ArrayList<Date> listTimesInCell; //List of all time marks in a cell -- update: option times in Date

    //aux representative MAT for ordenate
    private static List<Centroid> listRepPoint;

    private static MultipleAspectTrajectory representativeTrajectory; //Summarized MAT

    //aux to know the cell of each rp
    private String presentCell;

    // To create the Spatial summarization
    private double avgX, avgY;

    // To provide and compute runtime
    private Date initialTemp;

    private boolean dailyInfo;

    //Sum of each type of numerical attribute
    private static Map<String, List<Double>> sematicNumericFusionVal;
    //Sum of ocorrunces os each categorical attribute
    private static Map<String, Map<String, Integer>> sematicCategoricalSummarizationVal;

    // For validation
    private String filenameFullDataset; //Filename of the dataset
    private static List<MultipleAspectTrajectory> listAllTrajectories; //List of all MATs in the dataset

    
    //pattern to number
    DecimalFormat formatNumber = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
    
    
    
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

        //To add all types of semantic attributes in the dataset, specified in the first line
        int order = 0;
        for (String s : Arrays.copyOfRange(datasetColumns, INDEX_SEMANTIC, datasetColumns.length)) {
            //when attr do not need to be ignored
            if (lstIgnoreCols == null || !lstIgnoreCols.contains(s.toUpperCase().trim())) {
                if (lstCategoricalsPD.contains(s.toUpperCase())) //If attribute was predefined as categorical
                {
                    attributes.add(new SemanticAspect(s.toUpperCase(), order++, SemanticType.CATEGORICAL));
                } else {
                    attributes.add(new SemanticAspect(s.toUpperCase().trim(), order++));
                }
            } else {
                order++; //to skip column when it need to be ignored
            }

        }

        datasetRow = reader.readLine();

        //EoF - To get the trajectory data of dataset of each line
        while (datasetRow != null) {
//            System.out.println("linha: "+datasetRow);
            datasetColumns = datasetRow.toUpperCase().split(SEPARATOR);
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
        if (formatDate != null) {
            addTrajectoryData(attrValues[0], attrValues[1].split(" "), formatDate.parse(attrValues[2]), semantics);
        } else {
            addTrajectoryData(attrValues[0], attrValues[1].split(" "), Util.convertMinutesToDate(Integer.parseInt(attrValues[2])), semantics);
        }

    } // end of addAttributeValue method

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
            listTrajectories.add(new MultipleAspectTrajectory(Integer.parseInt(tId))); //Adds (Create) the new trajectory
            trajectory = listTrajectories.get(listTrajectories.size() - 1);
        }

        // aux values
        ArrayList<AttributeValue> attrs = new ArrayList<>();
        int ord = 0;
        SemanticAspect a;

        //Organizes the point semantic attributes
        for (String val : semantics) {
            a = findAttributeForOrder(ord++);
            if (a != null) {
                if (Arrays.asList(valuesNulls).contains(val)) { // Define as Unknown null values
                    val = "Unknown";
                } else if (a.getType() != null && a.getType().equals(SemanticType.CATEGORICAL)) { //if it is predefined as Categorical
                    val = "*" + val; // Use character '*' to force the number value to be a categorical value
                }

                attrs.add(new AttributeValue(val.toUpperCase(), a));

            }

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

    } //end addTrajectoryData method

    ///------
    //1st step of MATSG --> Spatial Segmentation
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
            //update on cell (spatial grid) adding this point
            rIds.set(p.getrId());
            spatialCellGrid.replace(key, rIds);
        }
    } //end allocateInSpaceCell method

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

    } // end getCellPosition method

    ///--------
    /// 2nd MATGS TimeSeq step --> Summarization
    /**
     * Analyse all valid cell and for each cell -- identify the points in cell
     * (add all object Point) -- identify and list all times, i.e. the temporal
     * value of each point -- then summarize temporal aspect (by
     * summarizeTemporalAspect method)
     */
    public void identifyTimesInCell() {

        //Create iterator object of all spatial grid cells
        Iterator<String> cell = spatialCellGrid.keySet().iterator();
        while (cell.hasNext()) {
            String cellAnalyzed = cell.next(); //Selects next cell

            //Gets amount of points in the current cell
            int qntPoints = spatialCellGrid.get(cellAnalyzed).cardinality();
//            System.out.println("Cell Analyzed: " + cellAnalyzed + " | points: " + qntPoints + " | threshold RC: " + threshold_rc);
            if (qntPoints >= threshold_rc) { // IF number is at least a threshold RC
                resetValuesToSummarization();

                // Loop in all points of the cell
                for (int pointId = spatialCellGrid.get(cellAnalyzed).nextSetBit(0);
                        pointId >= 0;
                        pointId = spatialCellGrid.get(cellAnalyzed).nextSetBit(pointId + 1)) {
                    pointsInCell.add(points.get(pointId - 1));

                    //Temporal data 
                    listTimesInCell.add(points.get(pointId - 1).getTime().getStartTime()); // update: add start time (in Date) of point in a list 
                }
                presentCell = cellAnalyzed;
                //Temporal data
                summarizeTemporalAspect(listTimesInCell);

            }
        }
    } // end identifyTimesInCell method

    /**
     * For each valid temporal interval in the cell, a representative point is
     * defined -- For each valid STI it is created a new representative point,
     * but the computation of -- data summarization (spatial and semantic) it is
     * computed further (on computeCentroid method) -- i.e.
     * summarizeTemporalAspect summarize the Temporal dimension in each cell --
     * then find and define Representative points for cell
     *
     * @param timeInPoints -- List of temporal information of all points in the
     * cell
     */
    public void summarizeTemporalAspect(ArrayList<Date> timeInPoints) {

        List<STI> listSTI = new ArrayList<>();

        //order times
        Collections.sort(timeInPoints);

        List<Integer> differences = new ArrayList<>(); //List of local time intervals

        //determining threshold by avg
        int threshold = 100; //the predefined threshold used when the time value is less than or equal to 2 occurrences

        //Begin -- Calculates the intervals of time
        float sumDifferences = 0, sumDifferencesSD = 0;
        for (int i = 1; i < timeInPoints.size(); i++) {
            int auxDif = (int) TimeUnit.MINUTES.convert(timeInPoints.get(i).getTime() - timeInPoints.get(i - 1).getTime(), TimeUnit.MILLISECONDS);

            if (auxDif > 0) {
                differences.add(auxDif);
                sumDifferences += differences.get(differences.size() - 1);
            }
        }

        //Average (Mean) temporal differences 
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
                    sumDifferencesSD += Math.pow(((int) differences.get(i) - avg), 2);
                }
                float SD = sumDifferencesSD / differences.size();
                SD = (float) Math.sqrt(SD);

                //3rd - compute the valid interval (the value of median of temporal differences  minus and plus (- / +) SD)
                float lessValue = med - SD;
                float upperValue = med + SD;

                //removing outliers:
                //remove values temporal differences less and upper the valid interval defined
                for (int i = 0; i < differences.size(); i++) {
                    if (differences.get(i) < lessValue) {
                        sumDifferences -= (int) differences.get(i);
                        differences.remove(differences.get(i));
                        i--;
                    } else if (differences.get(i) > upperValue) {
                        sumDifferences -= (int) differences.get(i);
                        differences.remove(differences.get(i));
                        i--;
                    }
                }

                // update threshold value to average value of temporal differences considering only valid values
                threshold = Math.floorDiv((int) sumDifferences, differences.size());
            } // end if more than 2 differences
        }//End computation of temporal threshold

        int cont = 1;
        TemporalAspect aspTime = null;
        //With the temporal threshold defined, it is defined the temporal valid intervals
        for (int i = 0; i < timeInPoints.size(); i++) {

            /*
            IF the occurrence is not the last one, 
            and two consecutive occurrences are considered a significant temporal interval (STI) -- ((NEW)) considering 10% GAP on threshold, 
            considering the threshold value, then it is considered a new valid interval
             */
            if ((i != timeInPoints.size() - 1)
                    && (TimeUnit.MINUTES.convert(timeInPoints.get(i).getTime(), TimeUnit.MILLISECONDS) + threshold)
                    >= (TimeUnit.MINUTES.convert(timeInPoints.get(i + 1).getTime(), TimeUnit.MILLISECONDS))) {

                try {
                    aspTime.getStartTime();
                } catch (NullPointerException e) {
                    aspTime = new TemporalAspect(timeInPoints.get(i)); // if not exist the object, it is created
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
                    aspTime.setEndTime(timeInPoints.get(i)); // if exist the object, the end time value is setted
//
                } catch (NullPointerException e) {
                    aspTime = new TemporalAspect(timeInPoints.get(i)); // if not exist the object, it is created
                }

                //Each STI contain the TemporalAspect (with the interval) and the proportion of this interval refers to the points in cell 
                listSTI.add(new STI(aspTime, (float) cont / timeInPoints.size()));//add occurrence or STI into rank list

                //reset aux values
                cont = 1;
                aspTime = null;
            }

        }

        // Ordernate temporal ranking 
        listSTI.sort(Comparator.comparing(STI::getProportion).reversed()); // order (DESC) STI by proportion

        // based into valid interval, it is created the Representative points for further compute them
        for (STI eachSTI : listSTI) {
            if (eachSTI.getProportion() >= threshold_rv) {
                Centroid repP = new Centroid();
                for (Point p : pointsInCell) {
                    if (eachSTI.getInterval().isInInterval(p.getTime().getStartTime())) {
                        repP.addPoint(p);
                    }
                }

                repP.setCellReference(presentCell);

                if (!repP.getPointListSource().isEmpty()) {
                    repP.setSti(eachSTI);
                    listRepPoint.add(repP);
                    representativeTrajectory.incrementValue(repP.getPointListSource().size());
                }
            }
        } //end loop in listSTI

    } //end summarizeTemporalAspect methid

    /**
     * Compute the representative point of each cell in the spatial grid,
     * last stage of summarization -- all aspects are summarized on it.
     */
    public void computeCentroid() {

        // Ordernate temporal ranking 
        listRepPoint = listRepPoint.stream().sorted().collect(Collectors.toList());
        for (Centroid representativePoint : listRepPoint) {
            resetValuesToSummarization();
            representativeTrajectory.addPoint(representativePoint);
            representativeTrajectory.incrementValue(representativePoint.getPointListSource().size());

            for (Point p : representativePoint.getPointListSource()) {
                // Spatial data
                avgX += p.getX();
                avgY += p.getY();

                //Semantic Data
                Double val;
                String attrActual; //storage attribute order to be used to get the object

                
                Double[] valuesNumInvalid = {-999.0, -1.0}; //Null values for numerical values

                for (AttributeValue atv : p.getListAttrValues()) {
                    attrActual = "" + atv.getAttibute().getOrder();

                    // numeric values - median computation 
                    //in this scope just create bitset with sum and count of values foreach quantitative attribute
                    try {

                        val = Double.parseDouble((String) atv.getValue()); // val -1 refers to empty value

                        if (!sematicNumericFusionVal.containsKey(attrActual)) {
                            sematicNumericFusionVal.put(attrActual, new ArrayList<Double>());
                        }

                        // Add to this key the attribute value if this value is not invalid
                        //i.e., if "val" not is contained in "valuesNumInvalid" 
                        if (!Arrays.asList(valuesNumInvalid).contains(val)) {
                            sematicNumericFusionVal.get(attrActual).add(val);
                        }

                    } catch (java.lang.NumberFormatException e) { //categorical values
                        /*
                            in this scope create the sematicCategoricalSummarizationVal with all possible values of each categorical attribute
                             and add its ids for after this step can computation the frequency of each one,
                             and identify the value more frequency of each qualitative attribute 
                         */

                        //IF not contains this key - attribute order
                        if (!sematicCategoricalSummarizationVal.containsKey(attrActual)) {
                            sematicCategoricalSummarizationVal.put(attrActual, new HashMap<String, Integer>());
                        }

                        // IF this attribute not contains this value
                        if (!sematicCategoricalSummarizationVal.get(attrActual).containsKey(atv.getValue())) {
                            //add this value to attribute and initialize the count
                            sematicCategoricalSummarizationVal.get(attrActual).put((String) atv.getValue(), 1);
                        } else {
                            sematicCategoricalSummarizationVal.get(attrActual).replace((String) atv.getValue(), sematicCategoricalSummarizationVal.get(attrActual).get(atv.getValue()) + 1);
                        }
                    }
                } //end FOR of all semantic attributes

            }// end FOR all points for interval time previous analysed 

            // Spatial data
            representativePoint.setSpatialDimension(avgX / representativePoint.getPointListSource().size(), avgY / representativePoint.getPointListSource().size());

            // ---- Semantic data
            //Loop for numeric attributes
            sematicNumericFusionVal.entrySet().forEach((entrada) -> {
                Double median = -999.0;
                Map<Object, Double> newMap = new HashMap<>();

                if (considerNulls) {
//                    
                    if ((representativePoint.getPointListSource().size() - entrada.getValue().size())
                            > entrada.getValue().size()) {
                        // When the size of null values (invalid values) is more than valid values
                        median = -999.0;
                        representativePoint.addAttrValue("" + median,
                                findAttributeForOrder(Integer.parseInt(entrada.getKey())));
                    } else if (representativePoint.getPointListSource().size() - entrada.getValue().size()
                            == entrada.getValue().size()) {
                        //When the size of null values and valid values are equals
                        newMap.put("-999.0", 0.5);
                        Collections.sort(entrada.getValue());

                        //Calculates the median value for all numeric attributes of the representative point
                        if (entrada.getValue().size() % 2 == 0) {
                            median = (entrada.getValue().get(entrada.getValue().size() / 2) + entrada.getValue().get(entrada.getValue().size() / 2 - 1)) / 2;
                        } else {
                            median = entrada.getValue().get(entrada.getValue().size() / 2);
                        }
                        newMap.put("" + median, 0.5);

                    } else {
                        //When the size of VALID values is more than the size of null values, 
                        //or equal proportion and is not consider null values
                        Collections.sort(entrada.getValue());
                        //Calculates the median value for all numeric attributes of the representative point
                        if (entrada.getValue().size() % 2 == 0) {
                            median = (entrada.getValue().get(entrada.getValue().size() / 2) + entrada.getValue().get(entrada.getValue().size() / 2 - 1)) / 2;
                        } else {
                            median = entrada.getValue().get(entrada.getValue().size() / 2);
                        }
                    }

                } else { // When not consider null values in computation (!considerNulls)
                    //Calculates the median value for all numeric attributes of the representative point 
                    // when the number of valid values is more (or equals) than null values
                    if (!entrada.getValue().isEmpty()
                            && (representativePoint.getPointListSource().size() - entrada.getValue().size())
                            <= entrada.getValue().size()) {
                        //Calculates the median value for all numeric attributes of the representative point
                        if (entrada.getValue().size() % 2 == 0) {
                            median = (entrada.getValue().get(entrada.getValue().size() / 2) + entrada.getValue().get(entrada.getValue().size() / 2 - 1)) / 2;
                        } else {
                            median = entrada.getValue().get(entrada.getValue().size() / 2);
                        }

                    }

                }

                //Set attribute value
                if (newMap.isEmpty()) {
                    representativePoint.addAttrValue("" + median,
                            findAttributeForOrder(Integer.parseInt(entrada.getKey())));
                } else {
                    representativePoint.addAttrValue(newMap,
                            findAttributeForOrder(Integer.parseInt(entrada.getKey())));
                }
                //Case of null median not is added (when not consider null values)

            });

            //begin -------- Loop for a categorical attributes
            for (Map.Entry<String, Map<String, Integer>> allCategorical : sematicCategoricalSummarizationVal.entrySet()) {
                Map<String, Integer> internalCategoricalList
                        = allCategorical.getValue().entrySet()
                                .stream()
                                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
                //Add mode value (tendency) of attribute to representative point

                if (considerNulls) {
                    representativePoint.addAttrValue(normalizeRankingValues(internalCategoricalList,
                            representativePoint.getPointListSource().size(), 's'),
                            findAttributeForOrder(Integer.parseInt(allCategorical.getKey()))
                    );
                } else {
                    representativePoint.addAttrValue(normalizeRankingValuesNotNulls(internalCategoricalList, representativePoint.getPointListSource().size(), 's'),
                            findAttributeForOrder(Integer.parseInt(allCategorical.getKey()))
                    );
                }
            } // end ------------ Loop for a categorical attributes

            //Reset values to representative point computation
            resetValuesToSummarization();
        }

    }

    /**
     * Identify all representative point of each cell in the spatial relevant cell grid. 
     * This stage not summarize semantic aspects (next stage).
     * 
     */
    public void findCentroid() {

        //Create iterator object of all spatial grid cells
        Iterator<String> cell = spatialCellGrid.keySet().iterator();
        while (cell.hasNext()) {
            String cellAnalyzed = cell.next(); //Selects next cell

            //Gets amount of points in the current cell
            int qntPoints = spatialCellGrid.get(cellAnalyzed).cardinality();
//            System.out.println("Cell Analyzed: " + cellAnalyzed + " | points: " + qntPoints + " | threshold RC: " + threshold_rc);
            if (qntPoints >= threshold_rc) { // IF number is at least a threshold RC
                resetValuesToSummarization();

                // Loop in all points of the cell
                for (int pointId = spatialCellGrid.get(cellAnalyzed).nextSetBit(0);
                        pointId >= 0;
                        pointId = spatialCellGrid.get(cellAnalyzed).nextSetBit(pointId + 1)) {
                    pointsInCell.add(points.get(pointId - 1));

                    //Temporal data 
                    listTimesInCell.add(points.get(pointId - 1).getTime().getStartTime()); // update: add start time (in Date) of point in a list 
                }
                presentCell = cellAnalyzed;
                //Temporal data
                defineRepPoints(listTimesInCell);

            }
        }
    }
    

    /**
     * Compute the the STIs for each representative cell -- i.e., define representative points that next will be summarized other aspects semantics.
     * @param timeInPoints 
     */
    public void defineRepPoints(ArrayList<Date> timeInPoints) {
        List<STI> listSTI = new ArrayList<>();

        //order times
        Collections.sort(timeInPoints);
        List<Integer> differences = new ArrayList<>(); //List of local time intervals

        //determine threshold by avg
        int threshold = 100; //the predefined threshold used when the time value is less than or equal to 2 occurrences

        //Begin -- Computation of the intervals of time
        float sumDifferences = 0, sumDifferencesSD = 0;
        for (int i = 1; i < timeInPoints.size(); i++) {
            //compute difference between each pair time values
            int auxDif = (int) TimeUnit.MINUTES.convert(timeInPoints.get(i).getTime() - timeInPoints.get(i - 1).getTime(), TimeUnit.MILLISECONDS);

            if (auxDif > 0) {
                differences.add(auxDif);
                sumDifferences += differences.get(differences.size() - 1);
            }
        }

        //Average temporal differences 
        float avg = sumDifferences / differences.size();

        if (differences.size() > 1) { //IF has more than 2 occurrences
            /*
            compute the valid interval to remove the outliers
             -- computation: valid interval median minus and plus (- / +) SD.
             */

            //order temporal differences
            Collections.sort(differences);

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
                    sumDifferencesSD += Math.pow(((int) differences.get(i) - avg), 2);
                }
                float SD = sumDifferencesSD / differences.size();
                SD = (float) Math.sqrt(SD);

                //3rd - compute the valid interval (the value of median of temporal differences  minus and plus (- / +) SD)
                float lessValue = med - SD;
                float upperValue = med + SD;

                //for removing outliers:
                //remove values of temporal differences less and upper the valid interval defined
                for (int i = 0; i < differences.size(); i++) {
                    if (differences.get(i) < lessValue || differences.get(i) > upperValue) {
                        sumDifferences -= (int) differences.get(i);
                        differences.remove(differences.get(i));
                        i--;
                    }
                }

                // update threshold value to average value of temporal differences considering only valid values
                threshold = Math.floorDiv((int) sumDifferences, differences.size());
            }
        }

        //End -- computation of temporal threshold (to define time interval to represent a STI
        int cont = 1;
        TemporalAspect aspTime = null;
        for (int i = 0; i < timeInPoints.size(); i++) {

            /*
            IF the occurrence is not the last one, 
            and two consecutive occurrences are considered a significant temporal interval (STI), 
            considering the threshold value, then it is considered a new valid interval
             */
            if ((i != timeInPoints.size() - 1)
                    && (TimeUnit.MINUTES.convert(timeInPoints.get(i).getTime(), TimeUnit.MILLISECONDS) + threshold)
                    >= (TimeUnit.MINUTES.convert(timeInPoints.get(i + 1).getTime(), TimeUnit.MILLISECONDS))) {

                try {
                    aspTime.getStartTime();
                } catch (NullPointerException e) { //IF the STI is null, a new one is created.
                    aspTime = new TemporalAspect(timeInPoints.get(i)); // if not exist the object, it is created
                }
                cont++;

            } /*
                IF it has only one occurrence, this is added into the rank list, or 
                if the occurence not is more considered into a previous STI 
             */ else {

                //on try scope --> get start time value -- 
                // else (catch scope) - not has the instance of object aspTime created, the NullPointer is apointed
                try {
                    aspTime.getStartTime();
                    aspTime.setEndTime(timeInPoints.get(i)); // if exist the object, the end time value is setted
//
                } catch (NullPointerException e) {
                    aspTime = new TemporalAspect(timeInPoints.get(i)); // if not exist the object, it is created
                }

                listSTI.add(new STI(aspTime, (float) cont / timeInPoints.size()));//add occurrence or STI into rank list
//                System.out.println("STI: " + aspTime + " points: " + cont + " times: " + timeInPoints.size());

                //reset aux values
                cont = 1;
                aspTime = null;
            }

        }

        // Ordernate temporal ranking 
        listSTI.sort(Comparator.comparing(STI::getProportion).reversed()); // order (DESC) STI by proportion

        // based into valid interval, it is created the Representative points for further compute them
        for (STI eachSTI : listSTI) {
            //VAnessa: mexi aqui: alter ">" to ">="
            if (eachSTI.getProportion() >= threshold_rv) {

                Centroid tempRepP = new Centroid();

                for (Point p : pointsInCell) {

                    if (eachSTI.getInterval().isInInterval(p.getTime().getStartTime())) {
                        tempRepP.addPoint(p);
                    }
                }

                tempRepP.setCellReference(presentCell);

                if (!tempRepP.getPointListSource().isEmpty()) { // IF this representative point have points (derived)
                    tempRepP.setSti(eachSTI);
                    listRepPoint.add(tempRepP);
//                    representativeTrajectory.incrementValue(tempRepP.getPointListSource().size());
                }
            }
        } //end loop in listSTI

    } // end defineRepPoints method

    
    
    /**
     * Compute the average of the minimum spatial distance of the input MATs
     * points to provide dynamic space segmentation for clustering these input
     * points. Given set of input MATs, with n points, we compute the Euclidean
     * distance d() for each point pi ∈ T with the nearest point pk ∈ T.
     *
     * the spatialThreshold is computed as the average of the minimum spatial --
     * distance the maximun Z value (diagonal size of each cell in the grid) is
     * computed as the max d() of the more distance point
     */
    public void computeSpatialThresholdOutliers() {
        float maxDistanceToZero = 0;

        float auxValueZ;

        float minDistance = 999999999999999999L;
        float localDistance;
        float sumDistance = 0;
        ArrayList<Float> listMinDistances = new ArrayList<>();
        for (Point p : points) {
            auxValueZ = Util.euclideanDistanceToZero(p);
            if (auxValueZ > maxDistanceToZero) {
                maxDistanceToZero = auxValueZ;
            }

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
                float lessValueMinDist = medianMinDist - 4 * sdMinDist;
                float upperValueMinDist = medianMinDist + 4 * sdMinDist;

                //for removing outliers:
                sumDistance = 0;
                //remove values temporal differences less and upper the valid interval defined
                for (int i = 0; i < listMinDistances.size(); i++) {
                    if (listMinDistances.get(i) < lessValueMinDist || listMinDistances.get(i) > upperValueMinDist
                            || listMinDistances.get(i) == 0.0) {
                        listMinDistances.remove(i);
                    } else {
                        sumDistance += listMinDistances.get(i);
                    }

                }
            }

            spatialThreshold = (sumDistance / listMinDistances.size());

//            System.out.println("Sum dist: " + sumDistance + " || qnt dist: " + listMinDistances.size());

            auxMaxZ = (maxDistanceToZero) / spatialThreshold;
//            System.out.println("Dist max: " + maxDistanceToZero + " || min dispersion: " + spatialThreshold + "|| Z max: " + auxMaxZ);

        }

    }

    /**
     * Method core to perform all methods in order to summarize input MATs into
     * one representative MAT.
     *
     * @param file name of file
     * @param ext extension of file
     * @throws IOException
     *
     */
    public void execute(String dir, String file, String ext, String[] lstCategoricalPD, String SEPARATOR, String[] valuesNULL, String[] ignoreColumns, String patternDateInput, float rc, float threshold_rv) throws IOException, ParseException, CloneNotSupportedException {
        initialTemp = new Date();
        //initialization of attribute values (Global attributes according to local data)
        directory = dir;
        filename = file;
        extension = ext;
        this.SEPARATOR = SEPARATOR;
        this.valuesNulls = valuesNULL;
        //Parameter for defining representativeness values and compute relevant cell
        this.threshold_rv = threshold_rv;

        lstCategoricalsPD = Arrays.asList(lstCategoricalPD);
        if (ignoreColumns != null) {
            lstIgnoreCols = Arrays.asList(ignoreColumns);
        }

        //initialization of object of MAT as representative MAT
        representativeTrajectory = new MultipleAspectTrajectory("representative");
        if (!patternDateInput.equals("?")) {
            this.formatDate = new SimpleDateFormat(patternDateInput);
        } else {
            this.representativeTrajectory.setDailyInfo(true);
            dailyInfo = true;
        }

        //initialization of aux attributes
        rId = 0;
        auxTid = "-1";
//        cId = -1;

        //initialization of aux lists
        listTimesInCell = new ArrayList<Date>();
        spatialCellGrid = new HashMap<String, BitSet>();
        sematicNumericFusionVal = new HashMap<String, List<Double>>();
        sematicCategoricalSummarizationVal = new HashMap<String, Map<String, Integer>>();
        points = new ArrayList<Point>();
        attributes = new ArrayList<SemanticAspect>();
        listTrajectories = new ArrayList<MultipleAspectTrajectory>();
        pointsInCell = new ArrayList<>();

        //aux representative MAT for ordenate
        listRepPoint = new ArrayList<>();

        // Load dataset follow data model representation
        load();

        //rc is defined as the minimun number of points ( calculated by the % of all points) that should have in each cell
        this.rc = rc;
        threshold_rc = rc > 0.0 ? (rc * points.size()) : 2; //If rc is greater than zero sets threshold according with number of points, else sets to 2

        
        listAllTrajectories = List.copyOf(listTrajectories);
        listTrajectories = new ArrayList<MultipleAspectTrajectory>();
        List<Point> auxClusterPoints = List.copyOf(points);
        points = new ArrayList<Point>();

        loadAllDataset();

        List<MultipleAspectTrajectory> auxDataset = List.copyOf(listTrajectories);
        listTrajectories = null;
        listTrajectories = List.copyOf(listAllTrajectories);
        listAllTrajectories = null;
        listAllTrajectories = List.copyOf(auxDataset);
        points = List.copyOf(auxClusterPoints);

        //######## automation - definition of better Z value - the spatial threshold
        // 1st - Calculates the spatial threshold according with the Z value and point dispersion
        computeSpatialThresholdOutliers();


        // 2nd - Summarize Trajectories into a single representative data
        summarizeTrajetories();


        




    }

    
    
    public void summarizeTrajetories() throws ParseException, CloneNotSupportedException {

        
        int tempMaxZ = (int) auxMaxZ, tempBetterZ = -1;
        float tempBetterRM = 0, tempZvalueRM = 0, iCoverZ = -1.0f, tempOnlyRM;

        String infoBetterRT = "";
        int count = 0;

        while (tempMaxZ > 1) {

            resetValuesRT();

            cellSizeSpace = (spatialThreshold * tempMaxZ) * 0.7071; // Calcultes size of the cells

            allocateAllPointsInCellSpace(); // Distributes all points in the spatial grid

            
            findCentroid(); //Creates the representative trajectory

            computeCentroid();

            if (!representativeTrajectory.getPointList().isEmpty()) {
                tempZvalueRM = (float) medianMeasureRecall();
                tempOnlyRM = tempZvalueRM;

                iCoverZ = (float) representativeTrajectory.getCoverPoints() / points.size();

//                tempZvalueRM = (tempZvalueRM * 0.5f) + (iCoverZ * 0.5f);
                tempZvalueRM = (tempZvalueRM + iCoverZ) /2; //*** melhor resultado

                
                  if ((tempZvalueRM * 1.1) >= tempBetterRM) { //***melhor resultado
                    tempBetterZ = tempMaxZ;
                    tempBetterRM = tempZvalueRM;
                    count = 0;
                    betterRT = null;
                    betterRT = (MultipleAspectTrajectory) representativeTrajectory.clone();
                    infoBetterRT = tempBetterZ + ", "
                            + betterRT.getPointList().size() + ", "
                            + cellSizeSpace + ", " + rc + ", "
                            + threshold_rc + ", " + threshold_rv + ", "
                            + spatialCellGrid.size() + ", "
                            + tempBetterRM + ", "
                            + betterRT.getCoverPoints() + ", "
                            + tempOnlyRM;

                } else {
                    count++;
                }

            }

            tempMaxZ *= 0.85;

            if (count > 1) {
                break;

            }
        } // fim do laço infinito - // Fim automação

        if (tempBetterZ > 1) {
            String outputFile = directory + "output\\" + filename + " rc " + (int) (rc * 100) + " rv " + (int) (threshold_rv * 100) + " - z" + tempBetterZ;
            writeRepresentativeTrajectory(outputFile, infoBetterRT);
            rank_MUITAS(outputFile);
        }
        
    }

    // #################### 
    // Normalizing our data by ratio value to generate data ranking
    
    
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
     * @param mappedPoints -- size of origins points of the representativePoint
     * @param dimension -- t: temporal and s: semantic
     * @return normalized -- the Map update with ratio values of occurrences.
     */
    public Map<Object, Double> normalizeRankingValues(Map<String, Integer> mapRank, int mappedPoints, char dimension) {

        Map<Object, Double> newMap = new HashMap<>();
        double trendEachVal;
        for (Map.Entry<String, Integer> eachValue : mapRank.entrySet()) {
            trendEachVal = (double) eachValue.getValue() / mappedPoints;
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
                    //when temporal dimens. refers to interval
                    auxInterval = formatDateOut.format(Util.convertMinutesToDate(Integer.parseInt(interval.substring(0, interval.indexOf("-")))));
                    auxInterval += " - " + formatDateOut.format(Util.convertMinutesToDate(Integer.parseInt(interval.substring(interval.indexOf("-") + 1))));
                } else {
                    //when it refers to a single occurrence
                    auxInterval = formatDateOut.format(Util.convertMinutesToDate(Integer.parseInt(interval)));
                }

                newTimeMap.put(auxInterval, newMap.get(interval));
            }
            return newTimeMap;
        } else { // Semantic dimension
            return newMapSorted;
        }

    } // end normalizeRankingValues method

    // 
    /**
     * For updating the number of occurrences of each rank value by the ratio
     * value, when user do not want consider null values on generation of
     * representative trajectory. Normalizing the Rank Value Map in the semantic
     * or temporal dimension, where the quantity of occurrences of each
     * attribute value is changed by the ratio of this value in relation to the
     * size of the cell.
     *
     * In temporal dimension the minutes values are converted to valid time
     * information.
     *
     * @param mapRank -- currently the Map of ranking values with number of
     * occurrences for each value
     * @param mappedPoints -- size of origins points of the representativePoint
     * @param dimension -- t: temporal and s: semantic
     * @return normalized -- the Map update with ratio values of occurrences.
     */
    public Map<Object, Double> normalizeRankingValuesNotNulls(Map<String, Integer> mapRank, int mappedPoints, char dimension) {

        Map<Object, Double> newMap = new HashMap<>();
        double trendEachVal;
        int sizeNotNull = mappedPoints;
        if (mapRank.containsKey("Unknown".toUpperCase())) {
//            System.out.println("Cell Size: " + mappedPoints);
            sizeNotNull -= mapRank.get("Unknown".toUpperCase()); //Sempre Zero????
//            System.out.println("Size Unknown: " + mapRank.get("Unknown".toUpperCase()));
//            System.out.println("Cell Size without null values: " + sizeNotNull);
        }
        if (sizeNotNull < mappedPoints) {
            for (Map.Entry<String, Integer> eachValue : mapRank.entrySet()) {
                if (!eachValue.getKey().equalsIgnoreCase("Unknown")) {
                    trendEachVal = (double) eachValue.getValue() / sizeNotNull;
                    if (trendEachVal >= threshold_rv) {
                        newMap.put(eachValue.getKey(), trendEachVal);
                    }
                }
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

    /**
     * allocate all points of input dataset in spatial cell grid
     */
    public void allocateAllPointsInCellSpace() {
        for (Point p : points) {
            allocateInSpaceCell(p);
        }
    }

    // --- MATSG - auxiliar methods -- to data reset, findAttribute...
    /**
     * Reset all values of all attributes in MAT
     */
    public void resetValuesToSummarization() {
        //Data reset

        //spatial data
//        avgX = 0;
//        avgY = 0;
//
//        // semantic data (multiple aspects)
//        sematicNumericFusionVal.clear();
//        sematicCategoricalSummarizationVal.clear();
        presentCell = "";
//
//        //temporal data
        listTimesInCell.clear();
        //all valid points in cell (used for temporal analysis)
        pointsInCell.clear();
//        
    } //end resetValuesToSummarization method

    /**
     * Reset data of values to compute the better RT
     */
    public void resetValuesRT() {
        spatialCellGrid.clear();
        pointsInCell.clear();
        presentCell = null;
        listTimesInCell.clear();
        listRepPoint.clear();
        representativeTrajectory = null;
        representativeTrajectory = new MultipleAspectTrajectory("representative");
        if (dailyInfo == true) {
            representativeTrajectory.setDailyInfo(true);
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
    } //end findAttributeForOrder method

    // Setter methods
    public void setSEPARATOR(String SEPARATOR) {
        this.SEPARATOR = SEPARATOR;
    }

    public void setINDEX_SEMANTIC(int INDEX_SEMANTIC) {
        this.INDEX_SEMANTIC = INDEX_SEMANTIC;
    }

    public void notConsiderNulls() {
        considerNulls = false;
    }

    // ----- Print data -- typing
    /**
     * Writes the generated representative trajectory in a new .csv file
     *
     * @param fileOutput -- output file name
     */
    public void writeRepresentativeTrajectory(String fileOutput, String infoBetterRT) {
        try {

            CSVWriter mxWriter = new CSVWriter(fileOutput + extension);
            mxWriter.writeLine("Method runtime information:");
            mxWriter.writeLine("Start timestamp: " + initialTemp);
            mxWriter.writeLine("End timestemp: " + new Date());
            mxWriter.writeLine("##");
            mxWriter.writeLine("Info input dataset:");
            mxWriter.writeLine("|input.T|, |input.T.points|");
            mxWriter.writeLine(listTrajectories.size() + ", " + points.size());
            mxWriter.writeLine("##");
            mxWriter.writeLine("RT setting infos:");
            mxWriter.writeLine("thresholdCellSize, |rt|, CellSize, "
                    + "tauRelevantCell, minPointsRC, "
                    + "tauRepresentativenessValue, |cell|, "
                    + "RepresentativenessMeasure, |cover RT|");
            mxWriter.writeLine(infoBetterRT);
            mxWriter.writeLine("##");
            mxWriter.writeLine("RT description:");
            for (Point p : betterRT.getPointList()) {
                mxWriter.writeLine(p.toString());
                mxWriter.flush();
            }

            mxWriter.flush();

            mxWriter.close();
        } catch (IOException e) {
//					Logger.log(Type.ERROR, pfx + e.getMessage());
            System.err.println("Error on writting RT: " + e.toString());
//e.printStackTrace();
        }
    }

    // ############## For automatization
    // ################# -- dinamicaly identify better cell size -- #################
    /**
     * Compute the Representativeness Measure by Median for Recall
     *
     * @return Representativeness Measure by Median for Recall
     * @throws ParseException
     */
    public double medianMeasureRecall() throws ParseException {
//        System.out.println("Measure: Median for Recall");

        if (representativeTrajectory.getPointList().isEmpty()) {
            System.out.println("RT zerada");
            return -1;
        }

//        SimilarityMeasure measure = new SimilarityMeasure();
        MUITAS measure = new MUITAS();
        
        //Compute thresholds
        //3D with equal weight (0.33) e totalizando 1.0
        measure.setWeight("SPATIAL", 0.34f);
        measure.setWeight("TIME", 0.33f);

        float auxWeight = 0.33f / (attributes.size());
//        System.out.println("Lista de semantic att: " + attributes);
        for (SemanticAspect eachAtt : attributes) {

            measure.setWeight(eachAtt, auxWeight);

        }
        
        
        double repMeasure = 0;
        List<Double> listValues = new ArrayList<>();

        for (MultipleAspectTrajectory eachTraj : listTrajectories) {

//            listValues.add(measure.recallOf(representativeTrajectory, eachTraj));
            listValues.add(measure.similarityOf(representativeTrajectory, eachTraj));

        }
        //after computed measure with each T and RT, it is computed median value
        Collections.sort(listValues);

        if (listValues.size() % 2 == 0) {

            repMeasure = (listValues.get(listValues.size() / 2) + listValues.get(listValues.size() / 2 - 1)) / 2;
        } else {
            repMeasure = listValues.get(listValues.size() / 2);
        }

        return repMeasure;

    }

    // ############## For validation #############
    private void loadAllDataset() throws IOException, ParseException {

        java.io.Reader input = new FileReader(directory + filenameFullDataset + extension);
        BufferedReader reader = new BufferedReader(input);

        String datasetRow = reader.readLine();

        datasetRow = reader.readLine();
        String[] datasetColumns;
        //EoF - To get the trajectory data of dataset of each line
        while (datasetRow != null) {
            datasetColumns = datasetRow.toUpperCase().split(SEPARATOR);
            addAttributeValues(datasetColumns);
            datasetRow = reader.readLine();
        }

        reader.close();

    }

    public void setFilenameFullDataset(String filenameFullDataset) {
        this.filenameFullDataset = filenameFullDataset;
    }

    public void rank_MUITAS(String fileOutput) throws ParseException {

        if (betterRT.getPointList().isEmpty()) {
            System.out.println("RT zerada");
            
        } else {

            MUITAS measure = new MUITAS();

            //Compute thresholds
            //3D with equal weight (0.33) e totalizando 1.0
            measure.setWeight("SPATIAL", 0.34f);
            measure.setWeight("TIME", 0.33f);

            float auxWeight = 0.33f / (attributes.size());

            for (SemanticAspect eachAtt : attributes) {

                measure.setWeight(eachAtt, auxWeight);

            }
            
            Map<Object, Double> rankMeasures = new HashMap<>();
            String infoMeasure = "";
            for (MultipleAspectTrajectory eachTraj : listAllTrajectories) {

                rankMeasures.put(eachTraj, measure.similarityOf(betterRT, eachTraj));

            }

            rankMeasures = rankMeasures.entrySet().stream()
                    .sorted(Map.Entry.<Object, Double>comparingByValue().reversed())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

            int countTclass = 0;
            int countPrecisionRetrivied = 0;
            int countRecallRetrivied = 0;

            for (Map.Entry<Object, Double> retriviedT : rankMeasures.entrySet()) {
                countPrecisionRetrivied++;
                infoMeasure
                        += ((MultipleAspectTrajectory) retriviedT.getKey()).getId() + ", "
                        + retriviedT.getValue() + ", "
                        + countPrecisionRetrivied + ", ";
                if (listTrajectories.contains((MultipleAspectTrajectory) retriviedT.getKey())) {
                    countTclass++;
                    infoMeasure += "1";
                    
                    if(countPrecisionRetrivied <= listTrajectories.size()){
                        countRecallRetrivied++;
//                        System.out.println("Entrou count Recall");
                    }
                } else {
                    infoMeasure += "0";
                }
                infoMeasure += "\n";
                if (countTclass == listTrajectories.size()) {
                    break;
                }
            }

//            System.out.println("|T_class| = " + countTclass);
//            System.out.println("|T_retrivied| = " + countPrecisionRetrivied);
//            System.out.println("Precision = " + ((double) countTclass / countPrecisionRetrivied));
//            System.out.println("|T_Recall_retrivied| = "+countRecallRetrivied);
//            System.out.println("Recall = " + ((double) countRecallRetrivied / listTrajectories.size()));

//        ---- write on a new file CSV each line the measure of each T and if this T is of the cluster used to compute RT
//        ---- next: write on a new File the rank of MUITAS --> id,measure,rank,clusterOfRT
            try {
                CSVWriter mxWriter = new CSVWriter(fileOutput + "[Validation]" + extension);
                mxWriter.writeLine("Method validation information:");
                mxWriter.writeLine("|Ground Truth|, |all input dataset|, "
                        + "|T.P.retrivied|, Precision, "
                        + "|T.R.retrivied|, Recall, F-Score");
                //vanessa: verificar se o calculo do Precision está correto, E se o calculo de similaridade está correto. Pois minha melhor Tr 5 - 15 deu 70% de média? entre todas do cluster e Precision de 10% só ?
                mxWriter.writeLine(listTrajectories.size() + ", " + listAllTrajectories.size() + ", "
                        + countPrecisionRetrivied + ", " + formatNumber.format((double) listTrajectories.size() / countPrecisionRetrivied ) + ", "
                        +countRecallRetrivied+", "+formatNumber.format((float)countRecallRetrivied / listTrajectories.size())+", ??");
                

                mxWriter.writeLine("##");
                mxWriter.writeLine("Measure description:");
                mxWriter.writeLine("Trajectory ID, MUITAS, #rank, Ground Truth?");
                mxWriter.writeLine(infoMeasure);
                mxWriter.flush();
                mxWriter.close();

                CSVWriter valWriter;

                String fileCompleteValidation = directory + "output\\" + filename + "[Validation]" + extension;
                if (!new File(fileCompleteValidation).exists()) {
                    valWriter = new CSVWriter(fileCompleteValidation);
                    valWriter.writeLine("Method validation information:");
                    valWriter.writeLine("Setting rv, Setting rc, "
                            + "|Ground Truth|, |all input dataset|, "
                            + "|T.P.retrivied|, Precision, "
                            + "|T.R.retrivied|, Recall, F-Score");

                } else {
                    valWriter = new CSVWriter(fileCompleteValidation, true);
                }
                valWriter.writeLine(formatNumber.format(threshold_rv) + ", " + formatNumber.format(threshold_rc / points.size()) + ", " 
                        + listTrajectories.size() + ", " + listAllTrajectories.size() + ", " 
                        + countPrecisionRetrivied + ", " + formatNumber.format((double) listTrajectories.size() / countPrecisionRetrivied ) + ", "
                        + countRecallRetrivied+", "+formatNumber.format((float)countRecallRetrivied / listTrajectories.size())+", ??");
                valWriter.flush();
                valWriter.close();

            } catch (IOException e) {
                System.err.println("Error on rank input trajectories X RT: " + e);
            }

        }

    }

}
