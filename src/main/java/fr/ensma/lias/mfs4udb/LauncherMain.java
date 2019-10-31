package fr.ensma.lias.mfs4udb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private static final double DEGREE_EXP = 0.9;
	
	protected String SHD_PATH = "/home/lias/shd";

    private MFS4UDBConfig config;

    private int occurenceNumber = 1;

    public LauncherMain() throws Exception {
	config = ConfigFactory.create(MFS4UDBConfig.class);
	occurenceNumber = Integer.valueOf(config.experimentalOccurenceNumber());

	Connection c = null;

	if (MFS4UDBConstants.MYSQL_NAME
		.equalsIgnoreCase(config.experimentalDatabaseType())) {
	    c = createMySQLConnection();
	} else if (MFS4UDBConstants.ORACLE_NAME
		.equalsIgnoreCase(config.experimentalDatabaseType())) {
	    c = createOracleConnection();
	} else if (MFS4UDBConstants.POSTGRESQL_NAME
		.equalsIgnoreCase(config.experimentalDatabaseType())) {
	    c = createPostgreSQLConnection();
	} else {
	    throw new NotYetImplementedException();
	}

	
	// save MFS and XSS for synthetic datasets with MDMB, and LBAwithMatrix
	executeExpSave1(c, "query18.input");
	executeExpSave2(c, "queries.input");
	
	// MBS with SHD on real datasets
//	executeExp0(c);

	// MBS with SHD on synthetic datasets nbr(MFS) vs dataset size
//	executeExp01(c, "query18.input");

	// MBS on synthetic datasets nbr(MFS) vs dataset size
//	executeExp1(c, "query18.input");

	// MBS with SHD nbr(MFS) on synthetic datasets vs query size 
//	executeExp2(c, "queries.input");

	// Algos on synthetic datasets (a,c,i) vs query size 
//  executeExp3(c, "queries.input");

	// Algos on synthetic datasets (a,c,i) vs dataset size
//	executeExp4(c, "query18.input"); 

	// Caracteristics of real dataset with LBA
//	executeExp6(c);

	// Algos on real datasets
//	executeExp7(c);
    }

	// Save MFS and XSS vs dataset size
    private void executeExpSave1(Connection c, String fileName) throws Exception {

    	List<MetaQuery> metaQueries = this.getMetaQueries(fileName);
    	if (metaQueries == null || metaQueries.size() == 0) {
    	    throw new NotYetImplementedException();
    	}
    	MetaQuery query = metaQueries.get(0);
    	int[] cards = { 0, 1, 2, 4, 8 };
    	char[] chars = { 'a', 'c', 'i' };

    	for (int i = 0; i < chars.length; i++) {
    	    System.out.println("-------- " + chars[i] + " ---------");
    	    BufferedWriter fichier1 = new BufferedWriter(
    		    new FileWriter("expSave1MFSwithMDMB-" + chars[i] + ".txt"));
			BufferedWriter fichier2 = new BufferedWriter(
    		    new FileWriter("expSave1MFSwithLBAwithMatrix-" + chars[i] + ".txt"));
			BufferedWriter fichier3 = new BufferedWriter(
    		    new FileWriter("expSave1XSSwithLBAwithMatrix-" + chars[i] + ".txt"));
    	    for (int j = 0; j < cards.length; j++) {
    		Query q = new Query(query.getContent(),
    			"lasttab" + cards[j] + chars[i], c);
    		
			List<Query> foundMFS1 = q.getAllMFSWithMDMB(DEGREE_EXP,(1+i), SHD_PATH);
    		fichier1.write("Nbr MFS for "+ cards[j] +" "+ chars[i]+ " est : " +foundMFS1.size() + "\n");
			for (Query qq : foundMFS1 ){
				fichier1.write(qq + "\n");
			}
    	    
			List<Query> foundMFS2 = q.getAllMFSWithLBA(DEGREE_EXP, true);
    		fichier2.write("Nbr MFS for "+ cards[j] +" "+ chars[i]+ " est : " + foundMFS2.size() + "\n");
			for (Query qq : foundMFS2 ){
				fichier2.write(qq + "\n");
			}
			
			List<Query> foundXSS = q.getAllXSSWithLBA(DEGREE_EXP, true, false);
    		fichier3.write("Nbr XSS for "+ cards[j] +" "+ chars[i]+ " est : " +foundXSS.size() + "\n");
			for (Query qq : foundXSS ){
				fichier3.write(qq + "\n");
			}
    	    }
    	    fichier1.close();
			fichier2.close();
			fichier3.close();
    	}
		
        }
		
	// Save MFS and XSS vs query size
    private void executeExpSave2(Connection c, String fileName) throws Exception {

	List<MetaQuery> metaQueries = this.getMetaQueries(fileName);
	if (metaQueries == null) {
	    throw new NotYetImplementedException();
	}
	char[] chars = { 'a', 'c', 'i' };

	for (int i = 0; i < chars.length; i++) {
	    System.out.println("-------- " + chars[i] + " ---------");
	    BufferedWriter fichier1 = new BufferedWriter(
		    new FileWriter("expSave2MFSwithMDMB-" + chars[i] + ".txt"));
		BufferedWriter fichier2 = new BufferedWriter(
    		    new FileWriter("expSave2MFSwithLBAwithMatrix-" + chars[i] + ".txt"));
			BufferedWriter fichier3 = new BufferedWriter(
    		    new FileWriter("expSave2XSSwithLBAwithMatrix-" + chars[i] + ".txt"));
	    for (MetaQuery query : metaQueries) {
		Query q = new Query(query.getContent(), "lasttab1" + chars[i],
			c);
		//List<Query> foundMFS = q.getAllMFSWithMCS(DEGREE_EXP, true);
    	List<Query> foundMFS1 = q.getAllMFSWithMDMB(DEGREE_EXP,(1+i), SHD_PATH);
		fichier1.write("Nbr MFS for "+ chars[i]+ " est : " +foundMFS1.size() + "\n");
		for (Query qq : foundMFS1 ){
				fichier1.write(qq + "\n");
			}
	    
		List<Query> foundMFS2 = q.getAllMFSWithLBA(DEGREE_EXP, true);
    		fichier2.write("Nbr MFS for "+ chars[i]+ " est : " +foundMFS2.size() + "\n");
			for (Query qq : foundMFS2 ){
				fichier2.write(qq + "\n");
			}
			
		List<Query> foundXSS = q.getAllXSSWithLBA(DEGREE_EXP, true, false);
			fichier3.write("Nbr XSS for "+ chars[i]+ " est : " +foundXSS.size() + "\n");
			for (Query qq : foundXSS ){
				fichier3.write(qq + "\n");
			}
		
		}
	    fichier1.close();
		fichier2.close();
		fichier3.close();
	}
    }
	
    // MBS with SHD on real datasets 
    private void executeExp0(Connection c) throws Exception {
    	System.out.println("-------- Exp 0 getAllMFSWithMDMB---------");
    	String[] fileNames = { "query8.input", "query6.input",
    		"query15.input" };
    	String[] tableNames = { "nba", "house", "weather" };
    	BufferedWriter fichier = new BufferedWriter(new FileWriter("exp0.csv"));
    	for (int i = 0; i < fileNames.length; i++) {
    	    List<MetaQuery> metaQueries = this.getMetaQueries(fileNames[i]);
    	    if (metaQueries == null || metaQueries.size() == 0) {
    		throw new NotYetImplementedException();
    	    }
    	    MetaQuery query = metaQueries.get(0);
    	    Query q = new Query(query.getContent(), tableNames[i], c);
    	    List<Query> foundMFS = q.getAllMFSWithMDMB(DEGREE_EXP, 0, SHD_PATH);
    	    fichier.write(foundMFS.size() + "\n");
    	}
    	fichier.close();
        }
    
	// MBS with SHD on synthetic datasets
    private void executeExp01(Connection c, String fileName) throws Exception {

    	List<MetaQuery> metaQueries = this.getMetaQueries(fileName);
    	if (metaQueries == null || metaQueries.size() == 0) {
    	    throw new NotYetImplementedException();
    	}
    	MetaQuery query = metaQueries.get(0);
    	int[] cards = { 0, 1, 2, 4, 8 };
    	char[] chars = { 'a', 'c', 'i' };

    	for (int i = 0; i < chars.length; i++) {
    	    System.out.println("-------- " + chars[i] + " ---------");
    	    BufferedWriter fichier = new BufferedWriter(
    		    new FileWriter("exp01-" + chars[i] + ".csv"));
    	    for (int j = 0; j < cards.length; j++) {
    		Query q = new Query(query.getContent(),
    			"lasttab" + cards[j] + chars[i], c);
    		List<Query> foundMFS = q.getAllMFSWithMDMB(DEGREE_EXP,(1+i), SHD_PATH);
    		fichier.write(foundMFS.size() + "\n");
    	    }
    	    fichier.close();
    	}
        }
    
    // MBS on synthetic datasets
    private void executeExp1(Connection c, String fileName) throws Exception {

	List<MetaQuery> metaQueries = this.getMetaQueries(fileName);
	if (metaQueries == null || metaQueries.size() == 0) {
	    throw new NotYetImplementedException();
	}
	MetaQuery query = metaQueries.get(0);
	int[] cards = { 0, 1, 2, 4, 8 };
	char[] chars = { 'a', 'c', 'i' };

	for (int i = 0; i < chars.length; i++) {
	    System.out.println("-------- " + chars[i] + " ---------");
	    BufferedWriter fichier = new BufferedWriter(
		    new FileWriter("exp1-" + chars[i] + ".csv"));
	    for (int j = 0; j < cards.length; j++) {
		Query q = new Query(query.getContent(),
			"lasttab" + cards[j] + chars[i], c);
		List<Query> foundMFS = q.getAllMFSWithMBS(DEGREE_EXP);
		fichier.write(foundMFS.size() + "\n");
	    }
	    fichier.close();
	}
    }

	// MBS with SHD nbr(MFS) on synthetic datasets (a,c,i)
    private void executeExp2(Connection c, String fileName) throws Exception {

	List<MetaQuery> metaQueries = this.getMetaQueries(fileName);
	if (metaQueries == null) {
	    throw new NotYetImplementedException();
	}
	char[] chars = { 'a', 'c', 'i' };

	for (int i = 0; i < chars.length; i++) {
	    System.out.println("-------- " + chars[i] + " ---------");
	    BufferedWriter fichier = new BufferedWriter(
		    new FileWriter("exp2-" + chars[i] + ".csv"));
	    for (MetaQuery query : metaQueries) {
		Query q = new Query(query.getContent(), "lasttab1" + chars[i],
			c);
		//List<Query> foundMFS = q.getAllMFSWithMCS(DEGREE_EXP, true);
    		List<Query> foundMFS = q.getAllMFSWithMDMB(DEGREE_EXP,(1+i), SHD_PATH);
		fichier.write(foundMFS.size() + "\n");
	    }
	    fichier.close();
	}
    }

	// Algos on synthetic datasets (a,c,i) vs query size 
    private void executeExp3(Connection c, String fileName) {

	List<MetaQuery> metaQueries = this.getMetaQueries(fileName);
	if (metaQueries == null) {
	    throw new NotYetImplementedException();
	}

	char[] chars = { 'a', 'c', 'i' };
	for (int i = 0; i < chars.length; i++) {
	    // LBA with Matrix
	    executeAlgo(c, metaQueries, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithLBA(DEGREE_EXP, true);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		    System.out.println("Temps Matrix : " + q.timeToComputeMatrix
			    + " " + q.getCardinalityMatrix() + " "
			    + q.getSizeInBytesMatrix());
		}
	    }, true, "LBA+M", chars[i]);

	    // LBA without Matrix
	    executeAlgo(c, metaQueries, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithLBA(DEGREE_EXP, false);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		    System.out.println("Nb query: " + q.getNbExecutedQuery() + " Nb cache : " + q.getNbRepetedQuery());
		}
	    }, true, "LBA", chars[i]);

	    // DFS
	    executeAlgo(c, metaQueries, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithDFS(DEGREE_EXP);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		    //System.out.println("Nb cache : " + q.getNbRepetedQuery());
		}
	    }, false, "DFS", chars[i]);

	    // MCS
	    executeAlgo(c, metaQueries, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithMCS(DEGREE_EXP, false);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		   System.out.println("Nb executed queries : " + q.getNbExecutedQuery());
		}
	    }, true, "MCS", chars[i]);

	    // MBS
	    executeAlgo(c, metaQueries, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithMBS(DEGREE_EXP);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		    //System.out.println("Nb cache : " + q.getNbRepetedQuery());
		}
	    }, false, "MBS", chars[i]);
    
	    // MBS and SHD
	    executeAlgo(c, metaQueries, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithMDMB(DEGREE_EXP, 3, SHD_PATH);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		    //System.out.println("Nb cache : " + q.getNbRepetedQuery());
		}
	    }, false, "MBS Trans", chars[i]);
	    
	}	
	
    }

    // Algos on synthetic datasets (a,c,i) vs dataset size
	// this exp should only use one query
    private void executeExp4(Connection c, String fileName) {

	List<MetaQuery> metaQueries = this.getMetaQueries(fileName);
	if (metaQueries == null || metaQueries.size() == 0) {
	    throw new NotYetImplementedException();
	}
	MetaQuery query = metaQueries.get(0);

	char[] chars = { 'a', 'c', 'i' };
	// char[] chars = {'i' };

	for (int i = 0; i < chars.length; i++) {
	    // LBA with Matrix
	    executeAlgo(c, query, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithLBA(DEGREE_EXP, true);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		    System.out.println("Temps Matrix : " + q.timeToComputeMatrix
			    + " " + q.getCardinalityMatrix() + " "
			    + q.getSizeInBytesMatrix());
		}
	    }, true, "LBA+M", chars[i]);

	    // LBA without Matrix
	    executeAlgo(c, query, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithLBA(DEGREE_EXP, false);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		    System.out.println("Nb query : " + q.getNbExecutedQuery() + " Nb cache : " + q.getNbRepetedQuery());
		}
	    }, true, "LBA", chars[i]);

	    // DFS
	    executeAlgo(c, query, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithDFS(DEGREE_EXP);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		   // System.out.println("Nb cache : " + q.getNbRepetedQuery());
		}
	    }, false, "DFS", chars[i]);

	    // DFS+M
	    if (chars[i] == 'i') {
		executeAlgo(c, query, new AlgoRelaxation() {
		    @Override
		    public List<Query> processAlgo(Query query)
			    throws Exception {
			return query.getAllMFSWithDFS(DEGREE_EXP, true);
		    }
		}, new LogRelaxation() {

		    @Override
		    public void displayAlgoInformation(Query q) {
			//System.out.println("Nb cache : " + q.getNbRepetedQuery());
		    }
		}, false, "DFS+M", chars[i]);
	    }

	    // MCS
	    executeAlgo(c, query, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithMCS(DEGREE_EXP, false);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		    System.out.println("Nb queries : " + q.getNbExecutedQuery());
		}
	    }, true, "MCS", chars[i]);

	    // MCS+M
	    if (chars[i] == 'i') {
		executeAlgo(c, query, new AlgoRelaxation() {
		    @Override
		    public List<Query> processAlgo(Query query)
			    throws Exception {
			return query.getAllMFSWithMCS(DEGREE_EXP, true);
		    }
		}, new LogRelaxation() {

		    @Override
		    public void displayAlgoInformation(Query q) {
			// System.out.println("Nb cache : " + q.getNbRepetedQuery());
		    }
		}, false, "MCS+M", chars[i]);
	    }

	    // MBS
	    executeAlgo(c, query, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithMBS(DEGREE_EXP);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		    //System.out.println("Nb cache : " + q.getNbRepetedQuery());
		}
	    }, false, "MBS", chars[i]);
	    
	    // MBS trans
	    executeAlgo(c, query, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithMDMB(DEGREE_EXP, 4, SHD_PATH);
		}
	    }, new LogRelaxation() {

		@Override
		public void displayAlgoInformation(Query q) {
		    //System.out.println("Nb cache : " + q.getNbRepetedQuery());
		}
	    }, false, "MBS Trans", chars[i]);
	}
    }

	// Caracteristics of real dataset with LBA
    private void executeExp6(Connection c) throws Exception {
	String[] fileNames = { "query8.input", "query6.input",
		"query15.input" };
	String[] tableNames = { "nba", "house", "weather" };
	BufferedWriter fichier = new BufferedWriter(new FileWriter("exp6.csv"));
	for (int i = 0; i < fileNames.length; i++) {
	    List<MetaQuery> metaQueries = this.getMetaQueries(fileNames[i]);
	    if (metaQueries == null || metaQueries.size() == 0) {
		throw new NotYetImplementedException();
	    }
	    MetaQuery query = metaQueries.get(0);
	    Query q = new Query(query.getContent(), tableNames[i], c);
	    List<Query> foundMFS = q.getAllMFSWithLBA(DEGREE_EXP,true);
	    fichier.write(foundMFS.size() + "\n");
	    System.out.println("-------- MFS EXP6 getAllMFSWithLBA -------------------------------------- ");
	    for(Query qq : foundMFS){
	    	System.out.println("-------- MFS "+qq);
	    }
	}
	fichier.close();
    }

	// Algos on real datasets
    private void executeExp7(Connection c) throws Exception {
	String[] fileNames = { "query8.input", "query6.input",
		"query15.input" };
	String[] tableNames = { "nba", "house", "weather" };

	for (int i = 0; i < fileNames.length; i++) {
	    List<MetaQuery> metaQueries = this.getMetaQueries(fileNames[i]);
	    if (metaQueries == null || metaQueries.size() == 0) {
		throw new NotYetImplementedException();
	    }
	    MetaQuery query = metaQueries.get(0);
	    Query q = new Query(query.getContent(), tableNames[i], c);
	    executeAlgos(c, query, tableNames[i]);
	}
    }

    interface AlgoRelaxation {
	List<Query> processAlgo(Query query) throws Exception;
    }

    interface LogRelaxation {
	void displayAlgoInformation(Query query) throws Exception;
    }

    private void executeAlgo(Connection connection, List<MetaQuery> queries,
	    AlgoRelaxation currentAlgo, LogRelaxation currentLog,
	    boolean displayAlgoLog, String algoName, char typeDataset) {
	try {
	    ExpResult expResult = new ExpResult(occurenceNumber);
	    int k = 4;
	    for (MetaQuery query : queries) {
		for (int i = 0; i < occurenceNumber + 1; i++) {
		    List<Query> mfs = new ArrayList<>();
		    Query q = new Query(query.getContent(),
			   "lasttab1" + typeDataset, connection);
		    //Query q = new Query(query.getContent(),
			//	    "lasttab1a" , connection);
		    float tps = (float) 0.0;
		    if (algoName.equals("DFS") && k > 12
			    && typeDataset != 'c') {
			tps = (float) 200.0;
		    } else {
			long begin = System.currentTimeMillis();
			mfs = currentAlgo.processAlgo(q);
			long end = System.currentTimeMillis();
			tps = ((float) (end - begin)) / 1000f;
		    }
		    System.out.println(tps + " " + mfs.size());
		    if (i > 0)
			expResult.addQueryResult(i - 1, query.getName(), tps);

		    if (displayAlgoLog) {
			currentLog.displayAlgoInformation(q);
		    }
		}
		k++;
	    }
	    expResult.toFile("exp3-" + algoName + "-" + typeDataset + ".csv");
	    System.out.println(expResult.toString());
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new NotYetImplementedException();
	}
    }

    private void executeAlgo(Connection connection, MetaQuery query,
	    AlgoRelaxation currentAlgo, LogRelaxation currentLog,
	    boolean displayAlgoLog, String algoName, char typeDataset) {
	try {
	    char[] datasets = { '0', '1', '2', '4', '8' };
	    ExpResult expResult = new ExpResult(occurenceNumber);
	    int k = 0;
	    for (char dataset : datasets) {
		for (int i = 0; i < occurenceNumber + 1; i++) {
		    List<Query> mfs = new ArrayList<>();
		    Query q = new Query(query.getContent(),
			    "lasttab" + dataset + typeDataset, connection);
		    //Query q = new Query(query.getContent(),
			//	    "lasttab1a", connection);
		    float tps = (float) 0.0;
		    if ((algoName.equals("DFS") && k > 1 && typeDataset != 'c')
			    || (algoName.equals("MCS") && k > 3
				    && typeDataset == 'i')
			    || (algoName.equals("LBA") && k > 3
				    && typeDataset == 'i')) {
			tps = (float) 200.0;
		    } else {
			long begin = System.currentTimeMillis();
			mfs = currentAlgo.processAlgo(q);
			long end = System.currentTimeMillis();
			tps = ((float) (end - begin)) / 1000f;
		    }
		    System.out.println(tps + " " + mfs.size());
		    if (i > 0)
			expResult.addQueryResultByDataset(i - 1,
				"" + dataset + typeDataset, tps);

		    if (displayAlgoLog) {
			currentLog.displayAlgoInformation(q);
		    }
		}
		k++;
	    }
	    expResult.toFileByDataset(
		    "exp4-" + algoName + "-" + typeDataset + ".csv");
	    System.out.println(expResult.toStringByDataset());
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new NotYetImplementedException();
	}
    }

    private void executeAlgos(Connection connection, MetaQuery query,
	    String tableName) {
	try {
	    AlgoRelaxation[] algos = { new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithDFS(DEGREE_EXP);
		}
	    }, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithMBS(DEGREE_EXP);
		}
	    }, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithLBA(DEGREE_EXP, false);
		}
	    }, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
		    return query.getAllMFSWithMCS(DEGREE_EXP, false);
		}
	    }, new AlgoRelaxation() {
		@Override
		public List<Query> processAlgo(Query query) throws Exception {
			 return query.getAllMFSWithMDMB(DEGREE_EXP, 7, SHD_PATH);
		}
		}
	    };
	    ExpResult expResult = new ExpResult(occurenceNumber);
	    for (AlgoRelaxation currentAlgo : algos) {
		for (int i = 0; i < occurenceNumber + 1; i++) {
		    List<Query> mfs = new ArrayList<>();
		    Query q = new Query(query.getContent(), tableName,
			    connection);
		    float tps = (float) 0.0;
		    long begin = System.currentTimeMillis();
		    mfs = currentAlgo.processAlgo(q);
		    long end = System.currentTimeMillis();
		    tps = ((float) (end - begin));
		    if (tableName.equals("weather"))
			tps = ((float) (end - begin)) / 1000f;
		    else
			tps = ((float) (end - begin));
		    System.out.println(tps + " " + mfs.size() + " nb query: " + q.getNbExecutedQuery() + " cache : " + q.getNbRepetedQuery());
		    System.out.println("Temps Matrix : " + q.timeToComputeMatrix
			    + " " + q.getCardinalityMatrix() + " "
			    + q.getSizeInBytesMatrix());
		    if (i > 0)
			expResult.addQueryResultByDataset(i - 1,
				"" + currentAlgo, tps);
		}
	    }
	    expResult.toFileByDataset("exp7-" + tableName + ".csv");
	    System.out.println(expResult.toStringByDataset());
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new NotYetImplementedException();
	}
    }

    private List<MetaQuery> getMetaQueries(String fileName) {
	BufferedReader in = null;

	try {
	    final InputStream fileUrl = LauncherMain.class.getResourceAsStream("/" + fileName);
	    in = new BufferedReader(new InputStreamReader(fileUrl));

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

    private Connection createMySQLConnection() {
	try {
	    Connection c = DriverManager
		    .getConnection("jdbc:mysql://localhost:3306/exp?"
			    + "user=root&password=password");

	    return c;
	} catch (SQLException e) {
	    e.printStackTrace();

	    throw new NotYetImplementedException();
	}
    }

    private Connection createPostgreSQLConnection() {
	try {
	    
	    Class.forName(config.postgresqlDriver());
	    Connection c = DriverManager.getConnection(
		    config.postgresqlUrl(), config.postgresqlLogin(),
		    config.postgresqlPassword());
	    return c;
	} catch (SQLException | ClassNotFoundException e)  {
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

    public static void main(String[] args) throws Exception {
	// TODO: handle params to custom launcher.
	new LauncherMain();
    }

    // private List<Query> previousFoundMFSs;

    // private void executeAlgo(Connection connection, List<MetaQuery> queries,
    // AlgoRelaxation currentAlgo, LogRelaxation currentLog,
    // boolean displayAlgoLog) {
    // try {
    // ExpResult expResult = new ExpResult(occurenceNumber);
    // for (MetaQuery query : queries) {
    // previousFoundMFSs = null;
    // for (int i = 0; i < occurenceNumber + 1; i++) {
    // Query q = new Query(query.getContent(), "lasttab0a",
    // connection);
    // long begin = System.currentTimeMillis();
    // List<Query> foundMFSs = currentAlgo.processAlgo(q);
    // long end = System.currentTimeMillis();
    // float tps = ((float) (end - begin)) / 1000f;
    //
    // if (previousFoundMFSs != null) {
    // System.out.println(foundMFSs.size() + " = "
    // + previousFoundMFSs.size() + " ?");
    // for (Query foundQuery : foundMFSs) {
    // if (!previousFoundMFSs.contains(foundQuery))
    // System.out.println("ERROR : la MFS "
    // + foundQuery
    // + " trouvée n'est pas dans les MFS calculée précédemment");
    // }
    // }
    // previousFoundMFSs = foundMFSs;
    //
    // if (i > 0)
    // expResult.addQueryResult(i - 1, query.getName(), tps);
    //
    // if (displayAlgoLog) {
    // currentLog.displayAlgoInformation(q);
    // }
    // }
    // }
    //
    // System.out.println(expResult.toString());
    // } catch (Exception e) {
    // e.printStackTrace();
    // throw new NotYetImplementedException();
    // }
    // }
}
