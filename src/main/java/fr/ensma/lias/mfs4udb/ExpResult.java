package fr.ensma.lias.mfs4udb;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Stephane JEAN
 */
public class ExpResult {

    private static int ID_TPS = 1;

    protected int nbExecutionQuery;

    protected Map<String, QueryResult[]> resultsByQuery;
    
    protected Map<String, QueryResult[]> resultsByDataset;

    public ExpResult(int nbExecutionQuery) {
	super();
	this.nbExecutionQuery = nbExecutionQuery;
	this.resultsByQuery = new LinkedHashMap<String, QueryResult[]>();
	this.resultsByDataset = new LinkedHashMap<String, QueryResult[]>();
    }
    
    public void addQueryResult(int indice, String name, float pTime) {
	QueryResult[] queryResults = resultsByQuery.get(name);
	if (queryResults == null)
	    queryResults = new QueryResult[nbExecutionQuery];
	queryResults[indice] = new QueryResult(pTime);
	resultsByQuery.put(name, queryResults);
    }
    
    public void addQueryResultByDataset(int indice, String name, float pTime) {
	QueryResult[] queryResults = resultsByDataset.get(name);
	if (queryResults == null)
	    queryResults = new QueryResult[nbExecutionQuery];
	queryResults[indice] = new QueryResult(pTime);
	resultsByDataset.put(name, queryResults);
    }
    
    private float getTpsMoyen(Map<String, QueryResult[]> results, String query) {
 	return getMetriqueMoyenAux(results, query, ID_TPS);
    }
    

    private static float round(float d, int decimalPlace) {
	BigDecimal bd = new BigDecimal(Float.toString(d));
	bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
	return bd.floatValue();
    }
    
    public String toString(Map<String, QueryResult[]> results) {
	StringBuffer res = new StringBuffer("");
	
	final Set<String> keySet = results.keySet();
	for(String current : keySet) {
	    res.append(current + ": ");
	    Float valTemps = round(getTpsMoyen(results, current), 2);
	    res.append(valTemps.toString().replace('.', ',') + "\n");   
	}
	
	return res.toString();
    }
    
    public String toString() {
	return toString(resultsByQuery);
    }
    
    public String toStringByDataset() {
	return toString(resultsByDataset);
    }
    

    public String toStringFile(Map<String, QueryResult[]> results) {
	StringBuffer res = new StringBuffer("");
	
	final Set<String> keySet = results.keySet();
	for(String current : keySet) {
	    Float valTemps = round(getTpsMoyen(results, current), 2);
	    res.append(valTemps.toString().replace('.', ',') + "\n");   
	}
	
	return res.toString();
    }
    

    
    public float getMetriqueMoyenAux(Map<String, QueryResult[]> results, String query, int idMetrique) {
	float res = 0;

	final QueryResult[] queryResults = results.get(query);
	
	for (QueryResult queryResult : queryResults) {
	    if (queryResult != null) {
		if (idMetrique == ID_TPS)
		    res += queryResult.getTps();
		else {
		    throw new NotYetImplementedException();
		}
	    }
	}
	
	return res / nbExecutionQuery;
    }
    
    
    
    public void toFile(String descriExp) throws Exception {
	BufferedWriter fichier = new BufferedWriter(new FileWriter(
		descriExp));
	fichier.write(toStringFile(resultsByQuery));
	fichier.close();
    }
    
    public void toFileByDataset(String descriExp) throws Exception {
	BufferedWriter fichier = new BufferedWriter(new FileWriter(
		descriExp));
	fichier.write(toStringFile(resultsByDataset));
	fichier.close();
    }
}
