package fr.ensma.lias.mfs4udb;

import org.roaringbitmap.RoaringBitmap;

/**
 * @author JEAN Stéphane
 */
public class Matrix {
    
    protected int nbPredicates;

    private RoaringBitmap[] values;

    public Matrix(int nbPredicates) {
	this.nbPredicates = nbPredicates;
	values = new RoaringBitmap[nbPredicates];
	for (int i = 0; i < values.length; i++) {
	    values[i] = new RoaringBitmap();
	}
    }

    public int getCardinality() {
	return values[0].getCardinality();
    }

    public int getSizeInBytes() {
	int res = 0;
	for (RoaringBitmap roaringBitmap : values) {
	    res += roaringBitmap.getSizeInBytes();
	}

	return res;
    }

    public RoaringBitmap getBitVector(int pi) {
	return values[pi];
    }

    public void setTi(int mu, int pi) {
	values[pi-1].add(mu);
    }

  
}
