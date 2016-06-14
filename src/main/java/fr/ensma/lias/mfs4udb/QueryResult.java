package fr.ensma.lias.mfs4udb;

/**
 * @author Stephane JEAN
 */
public class QueryResult {

    private float tps;

    private int nbRequete;

    public QueryResult(float tps, int nbRequete) {
	super();
	this.tps = tps;
	this.nbRequete = nbRequete;
    }

    public float getTps() {
	return tps;
    }

    public int getNbRequete() {
	return nbRequete;
    }
}
