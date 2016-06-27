package fr.ensma.lias.mfs4udb;

/**
 * @author JEAN Stéphane
 */
public class Predicate {

    private String property;
    
    private String predicateText;

    public Predicate(String text) {
	this.predicateText = text;
	property = predicateText.substring(0, predicateText.indexOf(" "));
    }

    public String getProperty() {
	return property;
    }

    @Override
    public String toString() {
	return predicateText;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result
		+ ((predicateText == null) ? 0 : predicateText.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Predicate other = (Predicate) obj;
	if (predicateText == null) {
	    if (other.predicateText != null)
		return false;
	} else if (!predicateText.equals(other.predicateText))
	    return false;
	return true;
    }
}
