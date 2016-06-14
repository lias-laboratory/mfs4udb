package fr.ensma.lias.mfs4udb;

/**
 * @author Mickael BARON
 */
public class MetaQuery {
    
    private String name;
    
    private String content;

    public MetaQuery(String name, String content) {
	super();
	this.name = name;
	this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
