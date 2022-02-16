package sswapService;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
                
                int shift = Integer.parseInt(subjectHashMap.get("maxShift"));
                for (int i = -shift; i <= shift; i++) {
                    
                    setObjectProperty("hasAddress", "Nuijamiestenkatu 123, Mikkeli");
                    setObjectProperty("hasImage", "https://images.pexels.com/photos/1131573/pexels-photo-1131573.jpeg");
                    setObjectProperty("numberOfPlaces", "3");
                    setObjectProperty("numberOfBedrooms", "3");
                    setObjectProperty("distanceToLake", "520");
                    setObjectProperty("nearestCityName", "Mikkeli");
                    setObjectProperty("distanceToCity", "1300");
                    setObjectProperty("bookerName", subjectHashMap.get("bookerName"));
                    setObjectProperty("bookingNumber", ""+(i+shift+1));
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(subjectHashMap.get("startOfBooking"));
                    Calendar c = Calendar.getInstance();
                    c.setTime(date);
                    c.add(Calendar.DATE, i);
                    setObjectProperty("start", c.getTime().toString());
                    c.add(Calendar.DATE, Integer.parseInt(subjectHashMap.get("numberOfDays")));
                    setObjectProperty("end", c.getTime().toString());
                    
                    if (i == shift) break; 
                    
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
                
                
                
                
//                SSWAPObject sswapObject = null;
//                sswapObject = assignObject(subject);
//                Iterator<SSWAPProperty> iterator = object.getProperties().iterator();
//                while (iterator.hasNext()) {
//                    SSWAPProperty property = iterator.next();
//                    SSWAPPredicate predicate = rigGraph.getPredicate(property.getURI());
//                    sswapObject.addProperty(predicate, "");
//                }
//                object = sswapObject;
//                subject.addObject(sswapObject);
                
        System.out.println("--- service ends...");
        
    }
    
    /**
     * Sets an object of a property
     * @param objProperty 
     * @param objValue 
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
