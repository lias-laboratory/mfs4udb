package fr.ensma.lias.mfs4udb;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Stephane JEAN
 */
public class ExpResult {

    private static int ID_TPS = 1;

    private static int ID_NB_REQUETE = 2;
    
    protected int nbExecutionQuery;

    protected Map<String, QueryResult[]> resultsByQuery;

    public ExpResult(int nbExecutionQuery) {
	super();
	this.nbExecutionQuery = nbExecutionQuery;
	this.resultsByQuery = new HashMap<String, QueryResult[]>();
    }
    
    public void addQueryResult(int indice, String name, float pTime, int nbRequete) {
	QueryResult[] queryResults = resultsByQuery.get(name);
	if (queryResults == null)
	    queryResults = new QueryResult[nbExecutionQuery];
	queryResults[indice] = new QueryResult(pTime, nbRequete);
	resultsByQuery.put(name, queryResults);
    }
    
    private float getTpsMoyen(String query) {
 	return getMetriqueMoyenAux(query, ID_TPS);
    }
    
    private float getRequeteMoyen(String query) {
 	return getMetriqueMoyenAux(query, ID_NB_REQUETE);
    }

    private static float round(float d, int decimalPlace) {
	BigDecimal bd = new BigDecimal(Float.toString(d));
	bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
	return bd.floatValue();
    }
    
    @Override
    public String toString() {
	StringBuffer res = new StringBuffer("");
	
	final Set<String> keySet = resultsByQuery.keySet();
	for(String current : keySet) {
	    res.append(current + ": ");
	    Float valTemps = round(getTpsMoyen(current), 2);
	    res.append(valTemps.toString().replace('.', ',') + "\t");
	    res.append(Math.round(getRequeteMoyen(current)));	    
	}
	
	return res.toString();
    }
    
    public float getMetriqueMoyenAux(String query, int idMetrique) {
	float res = 0;

	final QueryResult[] queryResults = resultsByQuery.get(query);
	
	for (QueryResult queryResult : queryResults) {
	    if (queryResult != null) {
		if (idMetrique == ID_TPS)
		    res += queryResult.getTps();
		else if (idMetrique == ID_NB_REQUETE)
		    res += queryResult.getNbRequete();
		else {
		    throw new NotYetImplementedException();
		}
	    }
	}
	
	return res / nbExecutionQuery;
    }
}
