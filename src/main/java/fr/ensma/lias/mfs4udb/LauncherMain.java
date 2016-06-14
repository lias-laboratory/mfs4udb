package fr.ensma.lias.mfs4udb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aeonbits.owner.ConfigFactory;

import fr.ensma.lias.mfs4udb.cfg.MFS4UDBConfig;

/**
 * @author Mickael BARON
 */
public class LauncherMain {

    private MFS4UDBConfig config;

    private int occurenceNumber = 5;
    
    public LauncherMain() {
	config = ConfigFactory.create(MFS4UDBConfig.class);
	occurenceNumber = Integer.valueOf(config.experimentalOccurenceNumber());
	
	Connection c = null;
	final List<MetaQuery> metaQueries = this.getMetaQueries();
	if (metaQueries == null) {
	    throw new NotYetImplementedException();
	}

	if (MFS4UDBConstants.MYSQL_NAME
		.equalsIgnoreCase(config.experimentalDatabaseType())) {
	    c = createMySQLConnection();
	} else if (MFS4UDBConstants.ORACLE_NAME
		.equalsIgnoreCase(config.experimentalDatabaseType())) {
	    c = createOracleConnection();
	} else {
	    throw new NotYetImplementedException();
	}

	// LBA with Matrix
	executeAlgo(c, metaQueries, new AlgoRelaxation() {
	    @Override
	    public List<Query> processAlgo(Query query) throws Exception {
		return query.getAllMFSWithLBA(0.4, true, true);
	    }
	}, new LogRelaxation() {

	    @Override
	    public void displayAlgoInformation(Query q) {
		System.out.println("Temps Matrix : " + q.timeToComputeMatrix
			+ " " + q.getCardinalityMatrix() + " "
			+ q.getSizeInBytesMatrix());		
	    }
	}, false);
	
	// LBA without Matrix
	executeAlgo(c, metaQueries, new AlgoRelaxation() {
	    @Override
	    public List<Query> processAlgo(Query query) throws Exception {
		return query.getAllMFSWithLBA(0.4, false,false);
	    }
	}, new LogRelaxation() {

	    @Override
	    public void displayAlgoInformation(Query q) {
		System.out.println("Nb cache : " + q.getNbRepetedQuery());	
	    }
	}, false);	
	
	// DFS
	executeAlgo(c, metaQueries, new AlgoRelaxation() {
	    @Override
	    public List<Query> processAlgo(Query query) throws Exception {
		return query.getAllMFSWithDFS(0.4);
	    }
	}, new LogRelaxation() {

	    @Override
	    public void displayAlgoInformation(Query q) {
		System.out.println("Nb cache : " + q.getNbRepetedQuery());	
	    }
	}, false);	
    }
    
    private Connection createMySQLConnection() {
	try {
	    Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/exp?" +
                "user=root&password=password");
	    
	    return c;
	} catch (SQLException e) {
	    e.printStackTrace();
	    
	    throw new NotYetImplementedException();
	}
    }
    
    private Connection createOracleConnection() {
	try {
	    Connection c = DriverManager.getConnection(
			"jdbc:oracle:thin:@localhost:1521:orcl", "jean", "pass");    
	    return c;
	} catch (SQLException e) {
	    e.printStackTrace();
	
	    throw new NotYetImplementedException();
	}
    }

    interface AlgoRelaxation {
	List<Query> processAlgo(Query query) throws Exception;
    }

    interface LogRelaxation {
	void displayAlgoInformation(Query query);
    }

    private void executeAlgo(Connection connection, List<MetaQuery> queries,
	    AlgoRelaxation currentAlgo, LogRelaxation currentLog, boolean displayAlgoLog) {
	try {
	    ExpResult expResult = new ExpResult(5);
	    for (MetaQuery query : queries) {	
		for (int i = 0; i < occurenceNumber; i++) {
		    Query q = new Query(query.getContent(), connection);
		    long begin = System.currentTimeMillis();
		    currentAlgo.processAlgo(q);
		    long end = System.currentTimeMillis();
		    float tps = ((float) (end - begin)) / 1000f;

		    expResult.addQueryResult(i, query.getName(), tps, q.getNbExecutedQuery());

		    if (displayAlgoLog) {
			currentLog.displayAlgoInformation(q);
		    }
		}
	    }
	    
	    System.out.println(expResult.toString());
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new NotYetImplementedException();
	}
    }

    private List<MetaQuery> getMetaQueries() {
	BufferedReader in = null;

	try {
	    final URL fileUrl = LauncherMain.class.getResource(
		    "/" + config.experimentalQueriesInputFilename());
	    final FileReader file = new FileReader(fileUrl.getFile());

	    in = new BufferedReader(file);
	    final Pattern pTest = Pattern.compile("# (.*)");

	    String line;
	    String name = null;
	    String query = null;

	    List<MetaQuery> queries = new ArrayList<MetaQuery>();
	    while ((line = in.readLine()) != null) {
		final Matcher mTest = pTest.matcher(line);
		if (mTest.matches()) {
		    addMetaQuery(queries, name, query);

		    name = mTest.group(1);
		    query = null;
		} else {
		    line = line.trim();
		    if (!line.isEmpty()) {
			query = line;
		    }
		}
	    }

	    addMetaQuery(queries, name, query);

	    return queries;
	} catch (Exception e) {
	    e.printStackTrace();

	    return null;
	} finally {
	    if (in != null) {
		try {
		    in.close();
		} catch (IOException e) {
		    e.printStackTrace();

		    return null;
		}
	    }
	}
    }

    private void addMetaQuery(List<MetaQuery> queries, String name,
	    String query) {
	if (name != null && query != null) {
	    MetaQuery newMetaQuery = new MetaQuery(name, query);
	    queries.add(newMetaQuery);
	}
    }

    public static void main(String[] args) throws FileNotFoundException {
	// TODO: handle params to custom launcher.
	new LauncherMain();
    }
}
