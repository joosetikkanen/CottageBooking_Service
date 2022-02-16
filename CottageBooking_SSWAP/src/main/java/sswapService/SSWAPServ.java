package sswapService;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.json.JSONObject;

import com.ibm.icu.util.Calendar;

import info.sswap.api.model.RIG;
import info.sswap.api.model.SSWAPIndividual;
import info.sswap.api.model.SSWAPObject;
import info.sswap.api.model.SSWAPPredicate;
import info.sswap.api.model.SSWAPProperty;
import info.sswap.api.model.SSWAPSubject;
import info.sswap.api.servlet.MapsTo;

public class SSWAPServ extends MapsTo {
    
    private RIG rigGraph;
    private HashMap<String, String> subjectHashMap = new HashMap<String, String>();
    private SSWAPObject object;
    private SSWAPSubject subject;
    private int objectCount = 0;
    private final String PATH_TO_DB = Initializer.getDB();

    /**
     * Types and predicates created in the Resource Invocation Graph (RIG) document.
     *
     * @param rig document within which to get/create the types and predicates
     */
    @Override
    protected void initializeRequest(RIG rig) {
        // if we need to check service parameters we could start here
        System.out.println("--- in service...");
        rigGraph = rig;
    }

    @Override
    protected void mapsTo(SSWAPSubject translatedSubject) throws Exception {
        
        object = translatedSubject.getObject();
        subject = translatedSubject;

        Iterator<SSWAPProperty> iterator = translatedSubject.getProperties().iterator();

        while (iterator.hasNext()) {
            SSWAPProperty property = iterator.next();

            SSWAPPredicate predicate = rigGraph.getPredicate(property.getURI());
            String lookupValue = getStrValue(translatedSubject,predicate);

            if (lookupValue == null) lookupValue = "";
            subjectHashMap.put(getStrName(property.getURI()), lookupValue);
        }

        doServiceLogic();
    }
    
    public void doServiceLogic() throws ParseException {
        
        System.out.println("Do query...");
        System.out.println(PATH_TO_DB);
        Model model = RDFDataMgr.loadModel(PATH_TO_DB) ;
        OntModelSpec ontModelSpec = OntModelSpec.OWL_DL_MEM;
        OntModel ontModel = ModelFactory.createOntologyModel(ontModelSpec, model);

        String queryString = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" + 
             "PREFIX : <localhost:8080/CottageBooking/res/cottage.owl#>\n" + 
             "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" + 
             "SELECT ?item ?address ?image ?placesForPeople ?bedroomCount ?lakeDistance ?city ?cityDistance \n" +
             "WHERE {  ?item rdf:type :Cottage.\n" + 
                
              "         ?item :hasNearestCity ?city.\n" +
              "         ?city :hasName \""+subjectHashMap.get("nearestCity")+"\".\n" +
              "         ?item :distanceToLake ?lakeDistance.\n" +
              "         FILTER (?lakeDistance < "+subjectHashMap.get("maxDistanceToLake")+"+1)" +
              "         ?item :distanceToNearestCity ?cityDistance.\n" +
              "         FILTER (?cityDistance < "+subjectHashMap.get("maxDistanceToNearestCity")+"+1)" +
              "         ?item :hasAddress ?address.\n" +
              "         ?item :hasBedrooms ?bedroomCount.\n" +
              "         FILTER (?bedroomCount > "+subjectHashMap.get("numberOfBedrooms")+"-1)" +
              "         ?item :hasCapacity ?placesForPeople.\n" +
              "         FILTER (?placesForPeople > "+subjectHashMap.get("numberOfPlaces")+"-1)" +
              "         ?item :hasImage ?image.\n" +
              
             "}";
        
        
        Dataset dataset = DatasetFactory.create(ontModel);
        Query q = QueryFactory.create(queryString);

        QueryExecution qexec = QueryExecutionFactory.create(q, dataset);
        ResultSet resultSet = qexec.execSelect();
        
        int shift = Integer.parseInt(subjectHashMap.get("maxShift"));
        
        System.out.println("Results: ---");
        int bookingNumber = 0;
        
        while(resultSet.hasNext()) { // Cottages
            QuerySolution row = resultSet.next();
            RDFNode nextItemId = row.get("item");
            System.out.print("ItemID is: "+nextItemId.toString()+".\n");
            
            
            for (int i = -shift; i <= shift; i++) { // Bookings per cottage
                
                Resource cottageRes = model.getResource(row.get("item").toString());
                StmtIterator iter = cottageRes.listProperties();
                boolean overlap = false;
                
                while (iter.hasNext()) { // Check previous reservations for overlap
                    Statement stmt = iter.nextStatement();
                    if (stmt.getPredicate().toString().endsWith("hasReservation")) {
                        
                        RDFNode reservation = stmt.getObject();

                        StmtIterator resProperties = reservation.asResource().listProperties();
                        String resStartDate = "";
                        String resEndDate = "";
                        
                        while (resProperties.hasNext()) {
                            
                            Statement resProperty = resProperties.nextStatement();

                            if (resProperty.getPredicate().toString().endsWith("startingDate")) {
                                
                                resStartDate = resProperty.getObject().toString();
                            } else if (resProperty.getPredicate().toString().endsWith("endDate")) {
                                
                                resEndDate = resProperty.getObject().toString();
                            }
                        }

                        Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(subjectHashMap.get("startingDay"));
                        String days = subjectHashMap.get("numberOfDays");
                        Calendar c = Calendar.getInstance();
                        c.setTime(startDate);
                        c.add(Calendar.DATE, i);
                        startDate = c.getTime();
                        c.add(Calendar.DATE, Integer.parseInt(days));
                        Date endDate = c.getTime();
                        Date resStart = new SimpleDateFormat("yyyy-MM-dd").parse(resStartDate.substring(0, resStartDate.indexOf("T")));
                        Date resEnd = new SimpleDateFormat("yyyy-MM-dd").parse(resEndDate.substring(0, resEndDate.indexOf("T")));
                        if (resEnd.after(startDate) && endDate.after(resStart)) {
                            overlap = true;
                        }
                        else {
                            // no overlap
                        }
                    }
                }
                
                if (overlap) continue;
                
                if (bookingNumber > 0) {
//                  SSWAPObject sswapObject = null;
//                  sswapObject = assignObject(subject);
//                  
//                  Iterator<SSWAPProperty> iterator = object.getProperties().iterator();
//                  while (iterator.hasNext()) {
//                      SSWAPProperty property = iterator.next();
//                      SSWAPPredicate predicate = rigGraph.getPredicate(property.getURI());
//                      sswapObject.addProperty(predicate, "");
//                  }
//                  object = sswapObject;
//                  subject.addObject(sswapObject);
                  addObject();
              }
                
                row.varNames().forEachRemaining(v -> {
                    
                    switch (v) {
                    case "address":
                        setObjectProperty("address", row.get(v).toString());
                        break;
                    case "image":
                        setObjectProperty("imageURL", row.get(v).toString());
                        break;
                    case "placesForPeople":
                        setObjectProperty("actualNumberOfPlaces", row.get(v).toString());
                        break;
                    case "bedroomCount":
                        setObjectProperty("actualNumberOfBedrooms", row.get(v).toString());
                        break;
                    case "lakeDistance":
                        setObjectProperty("actualDistanceToLake", row.get(v).toString());
                        break;
                    case "city": // City is an object property which has a name
                        Resource cityRes = model.getResource(row.get(v).toString());
                        StmtIterator cityProps = cityRes.listProperties();
                        while (cityProps.hasNext()) {
                            Statement stmt = cityProps.nextStatement();
                            if (stmt.getPredicate().toString().endsWith("hasName")) {
                                setObjectProperty("actualNearestCity", stmt.getObject().toString());
                            }
                        }
                        break;
                    case "cityDistance":
                        setObjectProperty("actualDistanceToNearestCity", row.get(v).toString());
                        break;
                    default:
                        break;
                    }
                });
                
                setObjectProperty("bookerName", subjectHashMap.get("name"));
                setObjectProperty("bookingNumber", ""+ ++bookingNumber);

                //String startDate = subjectHashMap.get("startingDay");
                //String days = subjectHashMap.get("numberOfDays");
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(subjectHashMap.get("startingDay"));
                Calendar c = Calendar.getInstance();
                c.setTime(date);
                c.add(Calendar.DATE, i);
                setObjectProperty("actualStartingDate", c.getTime().toString());
                c.add(Calendar.DATE, Integer.parseInt(subjectHashMap.get("numberOfDays")));
                setObjectProperty("actualEndDate", c.getTime().toString());
                
                
                
//                SSWAPObject sswapObject = null;
//                sswapObject = assignObject(subject);
//                
//                //if (i == shift) break;
//                
//                Iterator<SSWAPProperty> iterator = object.getProperties().iterator();
//                while (iterator.hasNext()) {
//                    SSWAPProperty property = iterator.next();
//                    SSWAPPredicate predicate = rigGraph.getPredicate(property.getURI());
//                    sswapObject.addProperty(predicate, "");
//                }
//                object = sswapObject;
//                subject.addObject(sswapObject);
                
            }
            
        }
   
        System.out.println("--- service ends...");
        
    }
    
    private void addObject() {
        SSWAPObject sswapObject = null;
        sswapObject = assignObject(subject);
        
        Iterator<SSWAPProperty> iterator = object.getProperties().iterator();
        while (iterator.hasNext()) {
            SSWAPProperty property = iterator.next();
            SSWAPPredicate predicate = rigGraph.getPredicate(property.getURI());
            sswapObject.addProperty(predicate, "");
        }
        object = sswapObject;
        subject.addObject(sswapObject);
    }
    
    /**
     * Sets an object of a property
     * @param var the name of the value
     * @param node the value
     */
    public void setObjectProperty(String objProperty, String objValue) {    
        Iterator<SSWAPProperty> iterator = object.getProperties().iterator();
        
        while (iterator.hasNext()) {
            SSWAPProperty property = iterator.next();
            SSWAPPredicate predicate = rigGraph.getPredicate(property.getURI());
            if (getStrName(property.getURI()).equals(objProperty)) {
                object.setProperty(predicate, objValue);
                break;
            }
        }
    }
    
    /**
     * Returns the string value for a property instance on an individual.
     * If more than one property instance exists, only one is (arbitrarily) chosen.
     * 
     * @param sswapIndividual the individual with the property
     * @param propertyURI the URI identifying the property (predicate)
     * @return the value as a string; null on any failure
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

}
