package sswapServiceMediator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;

import com.hp.hpl.jena.shared.JenaException;

import fr.inrialpes.exmo.align.impl.BasicAlignment;
import fr.inrialpes.exmo.align.impl.method.StringDistAlignment;
import info.sswap.api.http.HTTPProvider;
import info.sswap.api.model.DataAccessException;
import info.sswap.api.model.RDG;
import info.sswap.api.model.RIG;
import info.sswap.api.model.RRG;
import info.sswap.api.model.SSWAP;
import info.sswap.api.model.SSWAPGraph;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPResource;
import info.sswap.api.model.SSWAPSubject;

public class SSWAPMed {

    private String serviceUrl = "";
    private String queryResult;
    private HashMap<String, String> queryParams; 
    private JSONObject alignment;
    private String resourcePath;
    private String MYONTO_URI;// = "http://localhost:8080/CottageBooking_Mediator/res/mySSWAPServiceOntology.owl";

    public SSWAPMed(String resourcePath, String myOntoURI){
    
    System.out.println("-------------");    
    System.out.println("I am Mediator..."); 
    System.out.println("-------------");
    
    this.resourcePath = resourcePath;
    this.MYONTO_URI = myOntoURI;
    
    }

    /**
     * @param endPoint URL of the external booking service 
     * @param params User given query parameters
     * @param alignment User modified alignment, null in the initial query
     * @throws JSONException
     */
    public void sendRequest(String endPoint, HashMap<String, String> params, JSONObject alignment) throws JSONException {       
        serviceUrl = endPoint;
        System.out.println("Service URL: " + serviceUrl);
        queryParams = params;
        this.alignment = alignment;
      
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(serviceUrl);
        CloseableHttpResponse response = null;
        try {
            response = client.execute(httpGet);
        } catch (Exception e) {
            System.out.println("Error executing httpGet: " + e);
        }
        RDG rdg = null;
        
        try {
            URI uri = new URI(serviceUrl);
            rdg = SSWAP.getResourceGraph(response.getEntity().getContent(), RDG.class, uri);
        } catch (DataAccessException e1) {
            System.out.println("dataexc");
            e1.printStackTrace();
        } catch (IllegalStateException e1) {
            e1.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        SSWAPResource resource = rdg.getResource();    
      
        System.out.println("Resource name: " + resource.getName());
        System.out.println("Resource oneline description: " + resource.getOneLineDescription());
        System.out.println(resource.getURI());
        
        
        readRDG(rdg, true);

        try {
            client.close();
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    
    public SSWAPSubject getSubject(RDG rdg) {
        SSWAPResource resource = rdg.getResource();
        SSWAPGraph graph = resource.getGraph();
        SSWAPSubject subject = graph.getSubject();
        return subject;
    }
    
    
    public String readRDG(final RDG rdg, boolean sendRIG) throws JSONException {
        System.out.println("---");
        System.out.println("Read RDG...");
        System.out.println("---");
        SSWAPSubject subject = getSubject(rdg);

        Iterator<SSWAPProperty> iterator = subject.getProperties().iterator();
        System.out.println("Request properties:");
        String foreignOntoURI = "";
        while (iterator.hasNext()) {
            SSWAPProperty property = iterator.next();
            String lookupName = getStrName(property.getURI());
            System.out.println(""+lookupName);
            String uri = property.getURI().toString();
            foreignOntoURI = uri.substring(0, uri.indexOf('#'));
        }
        
        if (sendRIG) sendRIG(rdg, foreignOntoURI);
        
        return foreignOntoURI;
   }



    
    /**
     * @param rdg
     * @param foreignOntoURI
     * @throws JSONException
     */
    public void sendRIG(RDG rdg, String foreignOntoURI) throws JSONException {  
        System.out.println("---");
        System.out.println("Send RIG...");
        System.out.println("---");
        boolean errors = false;
        SSWAPSubject subject = getSubject(rdg);
        SSWAPResource resource = rdg.getResource();

        Iterator<SSWAPProperty> iterator = subject.getProperties().iterator();

        String lookupName = "";
        String lookupValue = "";
        
        boolean alignmentFileExists = readAlignment(foreignOntoURI);
        
        boolean confident = true; //if alignment aleady exists, then confident by default
        
        if (alignment == null) {
            
            
            System.out.println(foreignOntoURI);
            System.out.println(MYONTO_URI);
            
            
            confident = alignOntologies(foreignOntoURI);
            
        }
        
        if (!confident) { // If the automatic alignment results are not confident enough, prompt the user 
            alignment.put("prompt", true);
            queryResult = alignment.toString();
            return;
        }

        while (iterator.hasNext()) {
            
            SSWAPProperty property = iterator.next();
            SSWAPPredicate predicate = rdg.getPredicate(property.getURI());           
            
            lookupName = getStrName(property.getURI());

            switch (alignment.get(lookupName).toString()) { // The alignment has mapping foreign term -> my term
            case "nearestCity":
                subject.setProperty(predicate, queryParams.get("city"));
                break;
            case "actualNearestCity":
                subject.setProperty(predicate, queryParams.get("city"));
                break;
            case "startingDay":
                subject.setProperty(predicate, queryParams.get("startDate"));
                break;
            case "numberOfPlaces":
                subject.setProperty(predicate, queryParams.get("places"));
                break;
            case "maxDistanceToNearestCity":
                subject.setProperty(predicate, queryParams.get("cityDist"));
                break;
            case "numberOfBedrooms":
                subject.setProperty(predicate, queryParams.get("bedrooms"));
                break;
            case "maxShift":
                subject.setProperty(predicate, queryParams.get("shift"));
                break;
            case "maxDistanceToLake":
                subject.setProperty(predicate, queryParams.get("lakeDist"));
                break;
            case "name":
                subject.setProperty(predicate, queryParams.get("name"));
                break;
            case "bookerName":
                subject.setProperty(predicate, queryParams.get("name"));
                break;
            case "numberOfDays":
                subject.setProperty(predicate, queryParams.get("days"));
                break;
            default:
                break;
            }
        }
        
        if (!alignmentFileExists) saveAlignment(foreignOntoURI);
        
        iterator = subject.getProperties().iterator();
        while (iterator.hasNext()) {
            SSWAPProperty property = iterator.next();
            SSWAPPredicate predicate = rdg.getPredicate(property.getURI());
            lookupName = getStrName(property.getURI());
            lookupValue = getStrValue(subject,predicate);
            System.out.println(""+lookupName+" : "+lookupValue);
        }   

        if (errors) return;

        SSWAPGraph graph = resource.getGraph();
        graph.setSubject(subject);
        resource.setGraph(graph);

        RIG rig = resource.getRDG().getRIG();
        HTTPProvider.RRGResponse response = rig.invoke();
        RRG rrg = response.getRRG();

        showResults(rrg);
    }


    /**
     * Reads the alignment file of the given ontology
     * @param foreignOntoURI
     * @return Whether the alignment file for the given ontology was found
     * @throws JSONException
     */
    private boolean readAlignment(String foreignOntoURI) throws JSONException {
        
        System.out.println("----Reading alignment----");
        
        String fileName = foreignOntoURI.substring(foreignOntoURI.lastIndexOf('/')+1, foreignOntoURI.lastIndexOf('.')) + "_alignment";
        System.out.println(resourcePath + fileName);
        
        try {
            File alignmentFile = new File(resourcePath + fileName);
            Scanner s = new Scanner(alignmentFile);
            alignment = new JSONObject();
            while (s.hasNextLine()) {
                String line = s.nextLine();
                String foreignTerm = line.substring(0, line.indexOf(':')).trim();
                String myTerm = line.substring(line.indexOf(':')+1).trim();
                System.out.println(foreignTerm + " : " + myTerm);
                alignment.put(foreignTerm, myTerm);
            }
            s.close();
        } catch (FileNotFoundException e) {
            System.out.println("No alignment found");
            System.out.println("----");
            return false;
        }
        System.out.println("----");
        return true;
    }

    /**
     * Saves the alignment to a file .../alignments/[ontology name]_alignment
     * @param foreignOntoURI
     */
    private void saveAlignment(String foreignOntoURI){
     // Save alignment to file
        System.out.println("---- Saving to file ----");

        String fileName = foreignOntoURI.substring(foreignOntoURI.lastIndexOf('/')+1, foreignOntoURI.lastIndexOf('.')) + "_alignment";
        
        try {
            FileWriter writer = new FileWriter(resourcePath + fileName);
            Iterator<String> iter = alignment.keys();
          iter.forEachRemaining(k -> {
              try {
                writer.write(k + " : " +  alignment.getString(k) + "\n");
            } catch (IOException | JSONException e) {
                
                e.printStackTrace();
            }
          });
          writer.close();
          System.out.println("---- Saved successfully ----");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Does the automatic alignment with the help of AlignmentAPI and self made substring similarity alignment
     * @param foreignOntoURI
     * @return Whether the alignment results are confident or not
     */
    private boolean alignOntologies(String foreignOntoURI) {
               
        
        alignment = new JSONObject();
        
        boolean confident = false;
        
            
            try {
                HashMap<String, AbstractMap.SimpleEntry<String, Double>> alignmentApiResults = alignmentAPIAlignment(foreignOntoURI);
                HashMap<String, AbstractMap.SimpleEntry<String, Double>> substrSimilarityResults = subStringSimilarityAlignment(foreignOntoURI);
                
                // Aggregate Alignment API and Substring similarity alignment results by picking the alignment cells with highest confidence
                HashMap<String, AbstractMap.SimpleEntry<String, Double>> results;
                if (alignmentApiResults.size() > substrSimilarityResults.size()) {
                    results = alignmentApiResults;
                }
                else {
                    results = substrSimilarityResults;
                }
                
                confident = true;
                double highestConfidence = 0.0;
                System.out.println("---- Aggregated results ----");
                int count = 0;
                int api = 0;
                int substr = 0;
                for (String foreignTerm : results.keySet()) {
                    
                    AbstractMap.SimpleEntry<String, Double> alignmentApiResult = alignmentApiResults.get(foreignTerm);
                    AbstractMap.SimpleEntry<String, Double> substrSimilarityResult = substrSimilarityResults.get(foreignTerm);
                    if (alignmentApiResult == null) {
                        highestConfidence = substrSimilarityResult.getValue();
                        alignment.put(foreignTerm, substrSimilarityResult.getKey());
                        System.out.println("SUBSTR: " +foreignTerm + " | " + substrSimilarityResult.getKey() + " | " + highestConfidence);
                        substr++;
                    }
                    else if (substrSimilarityResult == null){
                        highestConfidence = alignmentApiResult.getValue();
                        alignment.put(foreignTerm, alignmentApiResult.getKey());
                        System.out.println("API: " +foreignTerm + " | " + alignmentApiResult.getKey() + " | " + highestConfidence);
                        api++;
                    }
                    else if (alignmentApiResult.getValue() > substrSimilarityResult.getValue()) {
                        highestConfidence = alignmentApiResult.getValue();
                        alignment.put(foreignTerm, alignmentApiResult.getKey());
                        System.out.println("API: " +foreignTerm + " | " + alignmentApiResult.getKey() + " | " + highestConfidence);
                        api++;
                    }
                    else {
                        highestConfidence = substrSimilarityResult.getValue();
                        alignment.put(foreignTerm, substrSimilarityResult.getKey());
                        System.out.println("SUBSTR: " + foreignTerm + " | " + substrSimilarityResult.getKey() + " | " + highestConfidence);
                        substr++;
                    }
                    
                    if (highestConfidence < 0.7) { // if any of the alignment cells have less than 0.7 confidence, the user will be prompted
                        confident = false;
                    }
                    count++;   
                }
                System.out.println(count);
                System.out.println("API: " + api);
                System.out.println("substr: " + substr);
                
                
            } catch (AlignmentException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
        
        return confident;
    }

    /**
     * Uses the Alignment API for automatic alignment (merged results of ngram distance and smoa distance)
     * @param foreignOntoURI
     * @return Result map with foreign term as a key and (my term + confidence value) as value 
     * @throws URISyntaxException
     * @throws AlignmentException
     */
    private HashMap<String, AbstractMap.SimpleEntry<String, Double>> alignmentAPIAlignment(String foreignOntoURI)
                                                                  throws URISyntaxException, AlignmentException {
        
        Properties params = new Properties();
        URI foreignOnto = new URI(foreignOntoURI);
        URI myOnto = new URI("file:///" + MYONTO_URI.replace("\\", "/"));
        // Run two different alignment methods (e.g., ngram distance and smoa)
        AlignmentProcess a1 = new StringDistAlignment();
        params.setProperty("stringFunction","smoaDistance");
        a1.init(foreignOnto,myOnto);
        a1.align((Alignment)null, params);
        AlignmentProcess a2 = new StringDistAlignment();
        a2.init (foreignOnto,myOnto);
        params = new Properties();
        params.setProperty("stringFunction","ngramDistance");
        a2.align((Alignment)null, params);
        // Trim above .5 and .7 respectively
        a1.cut(0.5);
        a2.cut(0.7);
        
        // Clone and merge alignments
        BasicAlignment a1a2 = (BasicAlignment)(a1.clone());
        a1a2.ingest(a2);
        
        // Access alignment Cells
        Iterator<Cell> iterator = a1a2.iterator();
        int count = 0;
        
        HashMap<String, AbstractMap.SimpleEntry<String, Double>> alignmentApiResults = new HashMap<>();
        
        System.out.println("---- Alignment API results ----");
        while (iterator.hasNext()) {
            
            ++count;
            
            Cell cell = iterator.next();
//            String cellStr = cell.toString();
//            String semantics = cell.getSemantics();
            String object1 = cell.getObject1().toString();
            String object2 = cell.getObject2().toString();
//            String relation = cell.getRelation().toString();
            String strength = ""+cell.getStrength();
            //System.out.print(cellStr + " | ");
            //System.out.print(semantics + " | ");
            String foreignTerm = object1.substring(object1.indexOf('#')+1, object1.length()-1);
            System.out.print(foreignTerm+ " | ");
            String myTerm = object2.substring(object2.indexOf('#')+1, object2.length()-1);
            System.out.print(myTerm+ " | ");
            //System.out.print(relation+ " | ");
            System.out.println(strength);

            AbstractMap.SimpleEntry<String, Double> alignmentApiResult = new AbstractMap.SimpleEntry<>(myTerm, Double.parseDouble(strength));
            alignmentApiResults.put(foreignTerm, alignmentApiResult);
        }
        System.out.println(count);
        return alignmentApiResults;
    }

    /**
     * Aligns the ontologies by using the substring similarity -function
     * @param foreignOntoURI
     * @return Result map with foreign term as a key and (my term + confidence value) as value
     */
    private HashMap<String, AbstractMap.SimpleEntry<String, Double>> subStringSimilarityAlignment(String foreignOntoURI) {
        
        int count;
        OntModel myOntoModel = getOntologyModel(MYONTO_URI);
        OntModel foreignOntoModel = getOntologyModel(foreignOntoURI);
        
        // Categorize properties by their domain class in both ontologies
        HashMap<String, List<String>> myTermsByClass = propertiesByClass(myOntoModel);
        HashMap<String, List<String>> foreignTermsByClass = propertiesByClass(foreignOntoModel);
                      
        HashMap<String, AbstractMap.SimpleEntry<String, Double>> substrSimilarityResults = new HashMap<>();
        HashMap<String, AbstractMap.SimpleEntry<String, Double>> mappedClasses = new HashMap<>();
        
        System.out.println("---- Substring similarity results ----");
        
        // First align classes
        System.out.println("-- Classes --");
        for (String foreignClass : foreignTermsByClass.keySet()) {
            
            double strength = 0.0;
            String bestMatch = "";
            
            for (String myClass : myTermsByClass.keySet()) {
                
                double similarity = substrSimilarity(foreignClass.toLowerCase().toCharArray(), myClass.toLowerCase().toCharArray(), foreignClass.length(), myClass.length());
                
                if (similarity > strength) {
                    strength = similarity;
                    bestMatch = myClass;
                }
            }
            
            System.out.println(foreignClass + " | " + bestMatch + " | " + strength);
            AbstractMap.SimpleEntry<String, Double> substrSimilarityResult = new AbstractMap.SimpleEntry<>(bestMatch, strength);
            
            substrSimilarityResults.put(foreignClass, substrSimilarityResult);
            mappedClasses.put(foreignClass, substrSimilarityResult);
        }
        
        count = 0;
        
        // After classes has been aligned, align properties by corresponding class alignment
        System.out.println("-- Properties by classes --");
        System.out.println("-- [foreign class] -> [my class] --");
        for (Entry<String, SimpleEntry<String, Double>> mapping : mappedClasses.entrySet()) {
            
           Iterator<String> iter1 = foreignTermsByClass.get(mapping.getKey()).iterator();
           while (iter1.hasNext()) {
               String foreignTerm = iter1.next();
               
               double strength = 0.0;
               String bestMatch = "";
               
               for (String myTerm : myTermsByClass.get(mapping.getValue().getKey())){
                                                      
                  double similarity = substrSimilarity(foreignTerm.toLowerCase().toCharArray(), myTerm.toLowerCase().toCharArray(), foreignTerm.length(), myTerm.length());
                  if (similarity > strength) {
                      strength = similarity;
                      bestMatch = myTerm;
                  }
                   
               }
               
               if (bestMatch.equals("")) { // If the classes were not aligned correctly, align foreign term to any of my terms
                   for (Resource myTermRes : myOntoModel.listSubjects().toList()) {
                       String myTerm = myTermRes.getLocalName();
                       double similarity = substrSimilarity(foreignTerm.toLowerCase().toCharArray(), myTerm.toLowerCase().toCharArray(), foreignTerm.length(), myTerm.length());
                       if (similarity > strength) {
                           strength = similarity;
                           bestMatch = myTerm;
                       }
                   }
                   
                   System.out.println("Could not find proper class alignment for :");
               }
               else {
                   System.out.println(mapping.getKey() + " -> " + mapping.getValue().getKey() + " :");
               }
               
               System.out.println(foreignTerm + " | " + bestMatch + " | " + strength);
               System.out.println("------------------------------------------");
               count++;
               
               AbstractMap.SimpleEntry<String, Double> substrSimilarityResult = new AbstractMap.SimpleEntry<>(bestMatch, strength);
               substrSimilarityResults.put(foreignTerm, substrSimilarityResult);
               
           }
            
        }
        
        System.out.println(count);
        return substrSimilarityResults;
    }

    /**
     * Maps properties to their domain class
     * @param ontoModel
     * @return the mapping {domainClass -> [properties]}
     */
    private HashMap<String, List<String>> propertiesByClass(OntModel ontoModel) {
        
        // Get classes
        HashMap<String, List<String>> termsByClass = new HashMap<>();
        ResIterator iter = ontoModel.listSubjects();
        while (iter.hasNext()) {
            Resource r = iter.nextResource();
            StmtIterator stmtIter = r.listProperties();
            while (stmtIter.hasNext()) {
                Statement stmt = stmtIter.nextStatement();
                
                if (stmt.getPredicate().getLocalName().equals("type") && stmt.getObject().asResource().getLocalName().equals("Class")) {
                    termsByClass.put(r.getLocalName(), new ArrayList<>());
                }
            }
        }
        
        // Map properties to their domain classes
        iter = ontoModel.listSubjects();
        while (iter.hasNext()) {
            Resource r = iter.nextResource();
            StmtIterator stmtIter = r.listProperties();
            while (stmtIter.hasNext()) {
                Statement stmt = stmtIter.nextStatement();
                
                if (stmt.getPredicate().getLocalName().equals("domain")){
                    termsByClass.get(stmt.getObject().asResource().getLocalName()).add(r.getLocalName());
                }
            }
        }
        return termsByClass;
    }

    /**
     * Calculates the substring similarity for two strings
     * @param X string 1
     * @param Y string 2
     * @param m length of string 1
     * @param n length of string 2
     * @return the similarity value
     */
    private double substrSimilarity(char[] X, char[] Y, int m, int n) {
        
        // LCSuff[i][j]
        // contains length of longest
        // common suffix of
        // X[0..i-1] and Y[0..j-1].
        // The first row and first
        // column entries have no
        // logical meaning, they are
        // used only for simplicity of program
        int LCStuff[][] = new int[m + 1][n + 1];
       
        // To store length of the longest
        // common substring
        int k = 0;
 
        // Following steps build
        // LCSuff[m+1][n+1] in bottom up fashion
        for (int i = 0; i <= m; i++)
        {
            for (int j = 0; j <= n; j++)
            {
                if (i == 0 || j == 0)
                    LCStuff[i][j] = 0;
                else if (X[i - 1] == Y[j - 1])
                {
                    LCStuff[i][j]
                        = LCStuff[i - 1][j - 1] + 1;
                    k = Integer.max(k, LCStuff[i][j]);
                }
                else
                    LCStuff[i][j] = 0;
            }
        }
        double numerator = 2 * k;
        double denominator = m + n;
        return numerator/denominator;
    }

    /**
     * Reads the ontology from given URI
     * @param ontoURI
     * @return Model of the ontology
     */
    private OntModel getOntologyModel(String ontoURI) {
        OntModel ontoModel =  ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
        try {
            InputStream in = FileManager.get().open(ontoURI);
            try {
                ontoModel.read(in, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            in.close();
        } catch (JenaException | IOException e) {
            System.err.println(e.getMessage());
        }
        return ontoModel;
    }

    /**
     * Shows the results from the RRG graph 
     * @param rrg the RRG graph where the results are taken from
     * @throws JSONException 
     */
    public void showResults(RRG rrg) throws JSONException {      
        System.out.println("---");
        System.out.println("Get RRG...");
        System.out.println("---");
        List<JSONObject> results = new ArrayList<>();
        
        SSWAPResource resource = rrg.getResource();
        SSWAPGraph graph = resource.getGraph();
        SSWAPSubject subject = graph.getSubject();
        Iterator<SSWAPObject> iteratorObjects =  subject.getObjects().iterator();
        int i = 1;
    
        while (iteratorObjects.hasNext()) {
            
            boolean error = false;
            HashMap<String, String> bookingResult = new HashMap<>();
            SSWAPObject object = iteratorObjects.next();
            System.out.println("Result: "+i+" -------------");
            Iterator<SSWAPProperty> iteratorProperties = object.getProperties().iterator();
            while (iteratorProperties.hasNext()) {
                SSWAPProperty property = iteratorProperties.next();
                SSWAPPredicate predicate = rrg.getPredicate(property.getURI());
                String lookupName = getStrName(property.getURI());
                String lookupValue = getStrValue(object,predicate);
                if (lookupValue == null) {
                    error = true;
                }
                System.out.println(""+lookupName+" : "+lookupValue);
                bookingResult.put(alignment.getString(lookupName), lookupValue);
            }
            if (!error) {
                results.add(new JSONObject(bookingResult));
            }
            
            i++;
        }
        
        StringBuilder resultSB = new StringBuilder();

        results.forEach(o -> {
            resultSB.append(o.toString());
            resultSB.append(',');
            
        });
        if (resultSB.length() > 0) {
            resultSB.deleteCharAt(resultSB.length()-1);
        }
        
        queryResult = resultSB.toString();
        
        
    }


    /**
     * Gets the value of a property from an individual
     * @param sswapIndividual the individual whose property it is
     * @param sswapPredicate the predicate of the property
     * @return the value of the property as String
     */
    private String getStrValue(SSWAPIndividual sswapIndividual, SSWAPPredicate sswapPredicate) {
        String value = null;
        SSWAPProperty sswapProperty = sswapIndividual.getProperty(sswapPredicate);
        if ( sswapProperty != null ) {
            value = sswapProperty.getValue().asString();
            if ( value.isEmpty() ) {
                value = null;
            }
        }
        return value;
    }

    /**
     * Gets the name of the property
     * @param uri uri of the property
     * @return name of the property
     */
    private String getStrName(URI uri) {
        String[] parts = uri.toString().split("#");
        return parts[1];
    }
    
    public String getResult(){
        return queryResult;
    }

    /**
     * Fetches the ontology of external booking service by reading its RDG, then deletes the alignment of that ontology
     * @param endPoint
     * @throws Exception
     */
    public void deleteAlignment(String endPoint) throws Exception {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(endPoint);
        CloseableHttpResponse response = null;
        try {
            response = client.execute(httpGet);
        } catch (Exception e) {
            System.out.println("Error executing httpGet: " + e);
        }
        RDG rdg = null;
        
        try {
            URI uri = new URI(endPoint);
            rdg = SSWAP.getResourceGraph(response.getEntity().getContent(), RDG.class, uri);
        } catch (DataAccessException e1) {
            System.out.println("dataexc");
            e1.printStackTrace();
        } catch (IllegalStateException e1) {
            e1.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        SSWAPResource resource = rdg.getResource();    
      
        System.out.println("Resource name: " + resource.getName());
        System.out.println("Resource oneline description: " + resource.getOneLineDescription());
        System.out.println(resource.getURI());
        
        
        String foreignOntoURI = readRDG(rdg, false);
        String fileName = foreignOntoURI.substring(foreignOntoURI.lastIndexOf('/')+1, foreignOntoURI.lastIndexOf('.')) + "_alignment";
        File aligmentFile = new File(resourcePath + fileName);
        
        if (aligmentFile.delete()) {
            System.out.println("Deleted the file: " + aligmentFile.getName());
        }
        else {
            System.err.println("File not found");
            client.close();
            response.close();
            throw new Exception("File not found");
            
        }

        try {
            client.close();
            response.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
        
    }
    
    
}
