package fr.ensma.lias.mfs4udb;

/**
 * @author Stephane JEAN
 */
public class QueryResult {

    private float tps;

    public QueryResult(float tps) {
	super();
	this.tps = tps;
    }

    public float getTps() {
	return tps;
    }

}
