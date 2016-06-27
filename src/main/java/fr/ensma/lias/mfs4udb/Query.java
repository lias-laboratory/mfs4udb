package fr.ensma.lias.mfs4udb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.roaringbitmap.RoaringBitmap;

/**
 * @author JEAN Stéphane
 */
public class Query {

    /**
     * The initial query on which an algo is launched
     */
    public static Query initialQuery;

    /**
     * The matrix used to avoid executing several queries
     */
    protected static Matrix matrix;

    /**
     * for experiment
     */
    protected static int nbExecutedQuery;

    protected static int nbRepetedQuery;

    protected String tableName;

    /**
     * Cache optimization
     */
    protected static List<Query> successfulCachedQueries;

    protected Map<Double, List<Query>> allMFS;

    protected Map<Double, List<Query>> allXSS;

    protected int cardinalityMatrix;

    private List<Predicate> predicates;

    private String queryText;
    private String predicatesText;

    protected Connection session;

    protected float sizeInBytesMatrix;

    protected float timeToComputeMatrix;

    public static int getNbRepetedQuery() {
	return nbRepetedQuery;
    }

    public Query(String text, String tableName, Connection c) {
	this.session = c;
	this.predicatesText = text;
	this.tableName = tableName;
	this.queryText = "SELECT * FROM " + tableName;
	if (!text.equals(""))
	    queryText += " WHERE " + text;
	this.allMFS = new HashMap<>();
	this.allXSS = new HashMap<>();
	computePredicates();
    }

    public void addPredicate(Predicate p) {
	predicates.add(p);
	updateQueryText();
    }

    public void computeMatrix(double degree) throws Exception {
	// 1 - computes the query
	String query = null;
	StringBuffer selectClause = new StringBuffer("SELECT DISTINCT ");
	StringBuffer fromClause = new StringBuffer(" FROM " + tableName + " ");
	StringBuffer whereClause = new StringBuffer("WHERE ");

	for (int i = 0; i < getSize(); i++) {
	    Predicate predicate = predicates.get(i);
	    if (i > 0) {
		selectClause.append(", ");
		whereClause.append(" OR ");
	    }
	    selectClause.append("CASE WHEN " + predicate + " AND "
		    + predicate.getProperty() + "_V >= " + degree
		    + " THEN 1 ELSE 0 END AS " + predicate.getProperty());
	    whereClause.append("(" + predicate + " AND "
		    + predicate.getProperty() + "_V >= " + degree + ")");
	}

	query = selectClause.append(fromClause).append(whereClause).toString();
	// System.out.println(query);

	// 2- execute the query and load the result in main memory
	fillMatrix(query);
    }

    // public void computeMatrixMySQL(double degree) throws Exception {
    // // 1 - computes the query
    // String query = null;
    // StringBuffer selectOuterClause = new StringBuffer("SELECT ");
    // StringBuffer selectClause = new StringBuffer(" ");
    // StringBuffer concat = new StringBuffer("SELECT DISTINCT CONCAT(");
    // StringBuffer fromClause = new StringBuffer(" FROM lasttab ");
    // StringBuffer whereClause = new StringBuffer("WHERE ");
    //
    // for (int i = 0; i < getSize(); i++) {
    // Predicate predicate = predicates.get(i);
    // if (i > 0) {
    // selectClause.append(", ");
    // selectOuterClause.append(", ");
    // concat.append(",");
    // whereClause.append(" OR ");
    // }
    // selectOuterClause.append(predicate.getProperty());
    // concat.append(
    // "CASE WHEN " + predicate + " AND " + predicate.getProperty()
    // + "_V >= " + degree + " THEN 1 ELSE 0 END");
    // selectClause.append("CASE WHEN " + predicate + " AND "
    // + predicate.getProperty() + "_V >= " + degree
    // + " THEN 1 ELSE 0 END AS " + predicate.getProperty());
    // whereClause.append("(" + predicate + " AND "
    // + predicate.getProperty() + "_V >= " + degree + ")");
    // }
    //
    // query = selectOuterClause.append(" FROM (").append(concat).append("), ")
    // .append(selectClause).append(fromClause).append(whereClause)
    // .append(") t").toString();
    // System.out.println(query);
    //
    // // 2- execute the query and load the result in main memory
    // fillMatrix(query);
    // }

    public List<Query> computePotentialXSS(Query mfs) {
	List<Query> res = new ArrayList<Query>();
	if (getSize() == 1)
	    return res;
	for (Predicate p : mfs.getPredicates()) {
	    Query q = new Query(predicatesText, tableName, session);
	    q.removePredicate(p);
	    res.add(q);
	}
	return res;
    }

    private void computePredicates() {
	predicates = new ArrayList<Predicate>();
	int indexOfWhere = queryText.indexOf("WHERE");
	if (indexOfWhere != -1) {
	    // hardcode the beginning of the predicates
	    String predicatesText = queryText.substring(indexOfWhere + 6);
	    int currentIndex = -1;
	    int indexOfAnd = predicatesText.indexOf("AND", currentIndex);
	    while (indexOfAnd != -1) {
		predicates.add(new Predicate(predicatesText
			.substring(currentIndex + 1, indexOfAnd - 1)));
		currentIndex = indexOfAnd + 3;
		indexOfAnd = predicatesText.indexOf("AND", currentIndex);
	    }
	    predicates.add(new Predicate(predicatesText
		    .substring(currentIndex + 1, predicatesText.length())));
	}
    }

    private String computePredicatesText(List<Predicate> listPredicates) {
	String res = "";
	for (int i = 0; i < listPredicates.size(); i++) {
	    Predicate predicate = listPredicates.get(i);
	    if (i == 0)
		res += predicate;
	    else
		res += " AND " + predicate;
	}
	return res;
    }

    public Query concat(Query queryToConcat) {

	List<Predicate> listPredicates = new ArrayList<Predicate>(predicates);
	listPredicates.addAll(queryToConcat.getPredicates());
	return new Query(computePredicatesText(listPredicates), tableName,
		session);
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Query other = (Query) obj;
	if (other.getSize() != this.getSize()) // same size
	    return false;
	if (!this.includes(other)) // and one is included in the other
	    return false;
	return true;
    }

    private void fillMatrix(String query) throws SQLException {
	matrix = new Matrix(getSize());
	Statement reqOracle = session.createStatement();
	// System.out.println(query);
	ResultSet rset = reqOracle.executeQuery(query);
	nbExecutedQuery++;
	int mu = 1;
	while (rset.next()) {
	    // pi which are set to true
	    for (int i = 1; i <= getSize(); i++) {
		if (rset.getInt(i) == 1)
		    matrix.setTi(mu, i);
	    }
	    mu++;
	}
	reqOracle.close();
    }

    public Query findAnMFS(double degree, boolean useMatrix) throws Exception {
	Query qPrim = new Query(predicatesText, tableName, session);
	Query qStar = new Query("", tableName, session);
	Query qTemp;
	Predicate p;
	for (int i = 0; i < getSize(); i++) {
	    p = qPrim.removePredicate();
	    qTemp = qPrim.concat(qStar);
	    if (!qTemp.isFailing(degree, useMatrix)) {
		qStar.addPredicate(p);
	    }
	}
	return qStar;
    }
    
    public List<Query> getAllMFSWithDFS(double degree) throws Exception {
	return getAllMFSWithDFS(degree, false);
    }

    public List<Query> getAllMFSWithDFS(double degree, boolean useMatrix) throws Exception {
	// if (allMFS.get(degree) == null) {
	runDFS(degree, useMatrix);
	// }
	return allMFS.get(degree);
    }

    public List<Query> getAllMFSWithMCS(double degree, boolean useMatrix)
	    throws Exception {
	// if (allMFS.get(degree) == null) {
	runMcSherry(this, degree, useMatrix);
	// }
	return allMFS.get(degree);
    }

    public List<Query> getAllMFSWithMBS(double degree) throws Exception {
	// if (allMFS.get(degree) == null) {
	runMBS(this, degree);
	// }
	return allMFS.get(degree);
    }

    public List<Query> getAllMFSWithLBA(double degree, boolean useMatrix)
	    throws Exception {
	// if (allMFS.get(degree) == null) {
	runLBA(degree, useMatrix);
	// }
	return allMFS.get(degree);
    }

    public List<Query> getAllXSSWithDFS(double degree) throws Exception {
	// if (allXSS.get(degree) == null) {
	runDFS(degree);
	// }
	return allXSS.get(degree);
    }

    public List<Query> getAllXSSWithLBA(double degree, boolean useMatrix,
	    boolean isMySQL) throws Exception {
	// if (allXSS.get(degree) == null) {
	runLBA(degree, useMatrix);
	// }
	return allXSS.get(degree);
    }

    public int getCardinalityMatrix() {
	return cardinalityMatrix;
    }

    public int getNbExecutedQuery() {
	return nbExecutedQuery;
    }

    public List<Predicate> getPredicates() {
	return predicates;
    }

    private int getSize() {
	return predicates.size();
    }

    public float getSizeInBytesMatrix() {
	return sizeInBytesMatrix;
    }

    public List<Query> getSubQueries() {
	List<Query> res = new ArrayList<Query>();
	for (Predicate p : getPredicates()) {
	    Query qNew = new Query(predicatesText, tableName, session);
	    qNew.removePredicate(p);
	    res.add(qNew);
	}
	return res;
    }

    public List<Query> getSuperQueries() {
	List<Query> res = new ArrayList<Query>();
	for (Predicate p : initialQuery.getPredicates()) {
	    if (!includes(p)) {
		Query qNew = new Query(predicatesText, tableName, session);
		qNew.addPredicate(p);
		res.add(qNew);
	    }
	}
	return res;
    }

    public float getTimeToComputeMatrix() {
	return timeToComputeMatrix;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((predicates == null) ? 0
		: new HashSet<Predicate>(predicates).hashCode());
	return result;
    }

    private boolean includes(Predicate p) {
	if (queryText.indexOf(p.toString()) == -1)
	    return false;
	return true;
    }

    private boolean includes(Query q) {
	for (Predicate p : q.getPredicates()) {
	    if (!includes(p))
		return false;
	}
	return true;
    }

    private void initDFS() {
	nbExecutedQuery = 0;
	initialQuery = this;
    }

    private void initLBA() {
	initialQuery = this;
	nbExecutedQuery = 0;
	nbRepetedQuery = 0;
	this.successfulCachedQueries = new LinkedList<>();
	// this.failingCachedQueries = new LinkedList<>();
    }

    public boolean isFailing(double degree) throws Exception {
	if (predicatesText.equals("")) {
	    return false; // we assume that the table is not empty
	}
	
	// First look in the cache
	for (Query qCache : successfulCachedQueries) {
	    if (qCache.includes(this)) {
		nbRepetedQuery++;
		return false;
	    }
	}
	// for (Query qCache : failingCachedQueries) {
	// if (this.includes(qCache)) {
	// nbRepetedQuery++;
	// System.out.println("cache success by failure!!!");
	// return true;
	// }
	// }

	boolean res = false;
	Statement stmt = session.createStatement();
	String query = queryText;
	for (Predicate predicate : predicates) {
	    query += " AND " + predicate.getProperty() + "_V >= " + degree;
	}
	ResultSet rset = stmt.executeQuery(query);
	nbExecutedQuery++;
	if (!rset.next()) {
	    res = true;
	    // failingCachedQueries.add(this);
	} else {
	    successfulCachedQueries.add(this);
	}
	rset.close();
	stmt.close();
	return res;
    }

    public boolean isFailing(double degree, boolean useMatrix)
	    throws Exception {
	if (useMatrix)
	    return isFailingWithMatrix(degree);
	else
	    return isFailing(degree);
    }

    private boolean isFailingForDFS(double degree,
	    Map<Query, Boolean> executedQueries, boolean useMatrix)
		    throws Exception {
	if (this.equals(initialQuery)) {
	    return true;
	}
	Boolean val = executedQueries.get(this);
	if (val == null) {
	    if (useMatrix)
		val = isFailingWithMatrix(degree);
	    else
		val = isFailingWithoutCache(degree);
	    executedQueries.put(this, val);
	}
	return val;
    }

    protected boolean isFailingWithMatrix(double degree) throws Exception {
	if (predicatesText.equals("")) {
	    return false; // we assume that the table is not empty
	}
	Predicate temp;
	List<Predicate> predicatesInitialQuery = initialQuery.getPredicates();
	RoaringBitmap currentVector = matrix.getBitVector(
		predicatesInitialQuery.indexOf(predicates.get(0)));
	if (getSize() == 1) {
	    return currentVector.isEmpty();
	}
	for (int i = 1; i < getSize(); i++) {
	    temp = predicates.get(i);
	    RoaringBitmap vectorTemp = matrix
		    .getBitVector(predicatesInitialQuery.indexOf(temp));
	    currentVector = RoaringBitmap.and(currentVector, vectorTemp);
	    if (currentVector.isEmpty()) {
		return true;
	    }
	}
	return false;
    }

    public boolean isFailingWithoutCache(double degree) throws Exception {

	if (predicatesText.equals(""))
	    return false;

	boolean res = false;
	Statement stmt = session.createStatement();
	String query = queryText;
	for (Predicate predicate : predicates) {
	    query += " AND " + predicate.getProperty() + "_V >= " + degree;
	}
	ResultSet rset = stmt.executeQuery(query);
	nbExecutedQuery++;
	if (!rset.next()) {
	    res = true;
	}
	rset.close();
	stmt.close();
	return res;
    }

    public boolean isIncludedInAQueryOf(List<Query> queries) {
	for (Query q : queries) {
	    if (q.includes(this))
		return true;
	}
	return false;
    }

    // add a new MFS and change the pxss accordingly
    private void refactor(Query qStar, List<Query> pxss, double degree) {
	List<Query> pxssPrim;
	for (ListIterator<Query> itQPrimPrim = pxss.listIterator(); itQPrimPrim
		.hasNext();) {
	    Query qPrimPrim = itQPrimPrim.next();
	    if (qPrimPrim.includes(qStar)) {
		itQPrimPrim.remove();
		pxssPrim = qPrimPrim.computePotentialXSS(qStar);
		for (Query qJ : pxssPrim) {
		    if (!qJ.isIncludedInAQueryOf(pxss)
			    && !qJ.isIncludedInAQueryOf(allXSS.get(degree))) {
			itQPrimPrim.add(qJ);
		    }
		}
	    }
	}
    }

    private Predicate removePredicate() {
	Predicate res = predicates.remove(0);
	updateQueryText();
	return res;
    }

    public void removePredicate(Predicate p) {
	predicates.remove(p);
	updateQueryText();
    }

    public void runDFS(double degree) throws Exception {
	runDFS(degree, false);
    }

    public void runDFS(double degree, boolean useMatrix) throws Exception {
	initDFS();
	if (useMatrix) {
	    long begin = System.currentTimeMillis();
	    computeMatrix(degree);
	    cardinalityMatrix = matrix.getCardinality();
	    sizeInBytesMatrix = matrix.getSizeInBytes();
	    long end = System.currentTimeMillis();
	    timeToComputeMatrix = ((float) (end - begin)) / 1000f;
	}
	List<Query> mfsQ = new ArrayList<Query>();
	List<Query> xssQ = new ArrayList<Query>();
	allMFS.put(degree, mfsQ);
	allXSS.put(degree, xssQ);

	List<Query> listQuery = new ArrayList<Query>();
	Map<Query, Boolean> executedQueries = new HashMap<Query, Boolean>();
	Map<Query, Boolean> markedQueries = new HashMap<Query, Boolean>();
	listQuery.add(this);
	while (!listQuery.isEmpty()) {
	    Query qTemp = listQuery.remove(0);
	    // System.out.println("Traitement de "
	    // + qTemp.toSimpleString(initialQuery));
	    if (!markedQueries.containsKey(qTemp)) {
		markedQueries.put(qTemp, true);
		List<Query> subqueries = qTemp.getSubQueries();
		if (qTemp.isFailingForDFS(degree, executedQueries,useMatrix)) {
		    // this is a potential MFS
		    // System.out.println("potential mfs");
		    boolean isMFS = true;
		    for (Query subquery : subqueries) {
			if (subquery.isFailingForDFS(degree, executedQueries,useMatrix))
			    isMFS = false;
		    }
		    if (isMFS)
			mfsQ.add(qTemp);
		} else { // Potential XSS
		    List<Query> superqueries = qTemp.getSuperQueries();
		    boolean isXSS = true;
		    for (Query superquery : superqueries) {
			if (!superquery.isFailingForDFS(degree,
				executedQueries,useMatrix))
			    isXSS = false;
		    }
		    if (isXSS) // && !qTemp.isEmpty())
			xssQ.add(qTemp);
		}
		listQuery.addAll(0, subqueries);
	    }

	}
    }

    public void runLBA(double degree, boolean useMatrix) throws Exception {
	if (useMatrix) {
	    long begin = System.currentTimeMillis();
	    computeMatrix(degree);
	    cardinalityMatrix = matrix.getCardinality();
	    sizeInBytesMatrix = matrix.getSizeInBytes();
	    long end = System.currentTimeMillis();
	    timeToComputeMatrix = ((float) (end - begin)) / 1000f;
	}
	initLBA();
	Query qPrim, qStarStar;
	List<Query> mfsQ = new ArrayList<Query>();
	List<Query> xssQ = new ArrayList<Query>();
	allMFS.put(degree, mfsQ);
	allXSS.put(degree, xssQ);
	List<Query> pxss = null;
	Query qStar = findAnMFS(degree, useMatrix);
	mfsQ.add(qStar);
	pxss = computePotentialXSS(qStar);
	while (!pxss.isEmpty()) {
	    qPrim = pxss.get(0);
	    if (!qPrim.isFailing(degree, useMatrix)) { // Q' is an XSS
		xssQ.add(qPrim);
		pxss.remove(qPrim);
	    } else { // Q' contains an MFS
		qStarStar = qPrim.findAnMFS(degree, useMatrix);
		mfsQ.add(qStarStar);
		refactor(qStarStar, pxss, degree);
	    }
	}
    }

    public String toSimpleString() {
	String res = "";
	for (Predicate predicate : predicates) {
	    res += predicate.getProperty() + "\t";
	}
	return res;
    }

    @Override
    public String toString() {
	return queryText;
    }

    // update the query text from the list of predicates
    private void updateQueryText() {
	predicatesText = computePredicatesText(predicates);
	queryText = "SELECT * FROM " + tableName;
	if (!predicatesText.equals(""))
	    queryText += " WHERE " + predicatesText;
    }

    // Ajout Chourouk

    // =========================DébutMcSherry===================================================
    // //

    public List<List<Integer>> calculInterCombinaisons(
	    List<List<Integer>> allCombinaisons, List<Integer> model,
	    List<List<Integer>> list, int o, int size) {
	List<List<Integer>> list2 = new ArrayList<List<Integer>>();
	for (int i = 0; i < list.size(); i++) {
	    List<Integer> inter = new ArrayList<Integer>();
	    inter = list.get(i);
	    int h = model.indexOf(inter.get(o - 2));
	    for (int m = h; m < (size - 1); m++) {
		List<Integer> bn = new ArrayList<Integer>();
		for (int j = 0; j < (o - 1); j++) {
		    bn.add(inter.get(j));
		}
		bn.add(model.get(m + 1));
		list2.add(bn);
		allCombinaisons.add(bn);
	    }
	}
	return list2;
    }

    public List<List<Integer>> calculAllCombinaisons(Query q) throws Exception {
	List<List<Integer>> allCombinaisons = new ArrayList<List<Integer>>();
	List<List<Integer>> list_cmb = new ArrayList<List<Integer>>();
	List<Integer> model = new ArrayList<Integer>();
	for (Predicate p : predicates) {
	    String text = p.getProperty();
	    model.add(Integer.valueOf(text.substring(1)));
	    List<Integer> val = new ArrayList<Integer>();
	    val.add(Integer.valueOf(text.substring(1)));
	    allCombinaisons.add(val);
	    list_cmb.add(val);
	}
	for (int o = 2; o < model.size(); o++) {
	    list_cmb = calculInterCombinaisons(allCombinaisons, model, list_cmb,
		    o, model.size());
	}
	// System.out.println("Nombre de combinaisons : " +
	// allCombinaisons.size()
	// + " pour " + predicates.size() + " prédicats ");
	return allCombinaisons;
    }

    public boolean isFailingMC(double degree, List<Integer> q1)
	    throws Exception {

	boolean res = false;
	Statement stmt = session.createStatement();
	String query = "SELECT * FROM " + tableName + " WHERE ";
	for (int i = 0; i < q1.size(); i++) {
	    Integer j = q1.get(i);
	    Predicate predicate;
	    if (i == 0) {
		predicate = predicates.get(j - 1);
		query += predicate + " AND " + predicate.getProperty()
			+ "_V >= " + degree;
	    } else {
		predicate = predicates.get(j - 1);
		query += " AND " + predicate + " AND " + predicate.getProperty()
			+ "_V >= " + degree;
	    }
	}
	ResultSet rset = stmt.executeQuery(query);
	nbExecutedQuery++;
	if (!rset.next()) {
	    res = true;
	}
	rset.close();
	stmt.close();
	return res;
    }

    public List<List<Integer>> explainer(List<List<Integer>> allCombinaisons,
	    double degree, boolean useMatrix) throws Exception {
	List<List<Integer>> listMFS = new ArrayList<List<Integer>>();
	while (allCombinaisons.size() > 0) {
	    List<Integer> deletions = new ArrayList<Integer>();
	    List<Integer> q1 = new ArrayList<Integer>();
	    q1 = allCombinaisons.get(0);
	    allCombinaisons.remove(0);
	    if (isFailing2(degree, useMatrix, q1)) {
		listMFS.add(q1);
		for (List<Integer> q2 : allCombinaisons) {
		    if (in(q1, q2)) {
			deletions.add(allCombinaisons.indexOf(q2));
		    }
		}
	    }
	    int decalage = 0;
	    for (Integer d : deletions) {
		allCombinaisons.remove(d - decalage);
		decalage++;
	    }
	}
	return listMFS;
    }

    public boolean in(List<Integer> q1, List<Integer> q2) {
	for (Integer i : q1) {
	    if (q2.indexOf(i) == -1)
		return false;
	}
	return true;
    }

    private void initMcSherry() {
	initialQuery = this;
	nbExecutedQuery = 0;
	nbRepetedQuery = 0;
	this.successfulCachedQueries = new LinkedList<>();
    }

    public void runMcSherry(Query q, double degree, boolean useMatrix)
	    throws Exception {
	if (useMatrix) {
	    long begin = System.currentTimeMillis();
	    computeMatrix(degree);
	    cardinalityMatrix = matrix.getCardinality();
	    sizeInBytesMatrix = matrix.getSizeInBytes();
	    long end = System.currentTimeMillis();
	    timeToComputeMatrix = ((float) (end - begin)) / 1000f;
	    // System.out.println("Temps passé Matrice : " +
	    // timeToComputeMatrix);
	}
	initMcSherry();
	List<List<Integer>> listMFS = new ArrayList<List<Integer>>();
	List<List<Integer>> allCombinaisons = new ArrayList<List<Integer>>();
	allCombinaisons = calculAllCombinaisons(q);
	listMFS = explainer(allCombinaisons, degree, useMatrix);
	List<Query> mfsQ = convert(listMFS);
	allMFS.put(degree, mfsQ);
    }

    public List<Query> convert(List<List<Integer>> list) {
	List<Query> res = new ArrayList<>(list.size());
	for (List<Integer> queryInteger : list) {
	    String text = "";
	    int i = 0;
	    for (Integer integer : queryInteger) {
		if (i > 0)
		    text += " AND ";
		text += "P" + integer + " < 0.1";
		i++;
	    }
	    res.add(new Query(text, tableName, session));
	}
	return res;
    }

    public boolean isFailing2(double degree, boolean useMatrix,
	    List<Integer> q1) throws Exception {
	if (useMatrix)
	    return isFailingWithMatrix2(degree, q1);
	else
	    return isFailingMC(degree, q1);
    }

    protected boolean isFailingWithMatrix2(double degree, List<Integer> q1)
	    throws Exception {
	RoaringBitmap currentVector = matrix.getBitVector(q1.get(0) - 1);
	if (q1.size() == 1) {
	    return currentVector.isEmpty();
	}
	for (int i = 1; i < q1.size(); i++) {
	    RoaringBitmap vectorTemp = matrix.getBitVector(q1.get(i) - 1);
	    currentVector = RoaringBitmap.and(currentVector, vectorTemp);
	    if (currentVector.isEmpty()) {
		return true;
	    }
	}
	return false;
    }

    // ============================================================================
    // //

    // =========================DébutApproche===================================================
    // //

    public List<List<Integer>> runMBS(Query q, double degree) throws Exception {
	List<List<Integer>> ssq = new ArrayList<List<Integer>>();
	ssq = calculssq(q, degree);
	List<List<Integer>> allCombinaisons = new ArrayList<List<Integer>>();
	allCombinaisons = calculAllCombinaisons(q);
	List<List<Integer>> listMFS = new ArrayList<List<Integer>>();
	listMFS = calculMFS(allCombinaisons, ssq);
	List<Query> mfsQ = convert(listMFS);
	allMFS.put(degree, mfsQ);
	return listMFS;
    }

    public List<List<Integer>> calculssq(Query q, double degree)
	    throws Exception {
	List<List<Integer>> ssq = new ArrayList<List<Integer>>();
	String query = null;
	StringBuffer selectClause = new StringBuffer("SELECT DISTINCT ");
	StringBuffer fromClause = new StringBuffer(" FROM " + tableName);
	StringBuffer whereClause = new StringBuffer(" WHERE ");

	for (int i = 0; i < getSize(); i++) {
	    Predicate predicate = predicates.get(i);
	    if (i > 0) {
		selectClause.append(", ");
		whereClause.append(" OR ");
	    }
	    selectClause.append("CASE WHEN " + predicate + " AND "
		    + predicate.getProperty() + "_V >= " + degree + " THEN "
		    + (i + 1) + " END AS " + predicate.getProperty());
	    whereClause.append("(" + predicate + " AND "
		    + predicate.getProperty() + "_V >= " + degree + ")");
	}
	query = selectClause.append(fromClause).append(whereClause).toString();
	// System.out.println(query);
	Statement reqOracle = session.createStatement();
	ResultSet rsett = reqOracle.executeQuery(query);
	while (rsett.next()) {
	    List<Integer> s = new ArrayList<Integer>();
	    for (int i = 1; i <= getSize(); i++) {

		if (rsett.getInt(i) != 0) {
		    s.add(rsett.getInt(i));
		}
	    }
	    boolean ajout = true;
	    for (int i = 0; i < ssq.size(); i++) {
		List<Integer> l = new ArrayList<Integer>();
		l = ssq.get(i);
		if (l.size() < s.size()) {
		    if (in(l, s)) {
			ssq.remove(ssq.indexOf(l));
		    }
		} else {
		    if (in(s, l)) {
			ajout = false;
		    }
		}
	    }
	    if (ajout)
		ssq.add(s);
	}
	reqOracle.close();
	return ssq;
    }

    public List<List<Integer>> calculMFS(List<List<Integer>> allCombinaisons,
	    List<List<Integer>> ssq) throws Exception {
	suppressionSSQ(allCombinaisons, ssq);
	suppression(allCombinaisons);
	return allCombinaisons;
    }

    public void suppressionSSQ(List<List<Integer>> allCombinaisons,
	    List<List<Integer>> ssq) throws Exception {
	// System.out.println(" taille total ssq " + ssq.size());
	for (List<Integer> l : ssq) {
	    int pos = allCombinaisons.indexOf(l);
	    if (pos != -1) {
		allCombinaisons.remove(pos);
	    }
	    if (l.size() == 2) {
		List<Integer> ll = new ArrayList<Integer>();
		ll.add(l.get(0));
		pos = allCombinaisons.indexOf(ll);
		if (pos != -1) {
		    allCombinaisons.remove(pos);
		}
		ll.clear();
		ll.add(l.get(1));
		pos = allCombinaisons.indexOf(ll);
		if (pos != -1) {
		    allCombinaisons.remove(pos);
		}
	    } else if (l.size() > 2) {
		List<List<Integer>> allComb = new ArrayList<List<Integer>>();
		allComb = calculAllCombinaisons2(l);
		for (List<Integer> c : allComb) {
		    pos = allCombinaisons.indexOf(c);
		    if (pos != -1) {
			allCombinaisons.remove(pos);
		    }
		}
	    }
	}
	// System.out.println(" *********** ");
    }

    public void suppression(List<List<Integer>> allCombinaisons) {
	int i = 0;
	while (i < (allCombinaisons.size() - 1)) {
	    int j = i + 1;
	    while (j < allCombinaisons.size()) {
		if (in(allCombinaisons.get(i), allCombinaisons.get(j))) {
		    allCombinaisons.remove(j);
		} else
		    j++;
	    }
	    i++;
	}
    }

    public List<List<Integer>> calculAllCombinaisons2(List<Integer> q)
	    throws Exception {
	List<List<Integer>> allCombinaisons = new ArrayList<List<Integer>>();
	List<List<Integer>> list_cmb = new ArrayList<List<Integer>>();
	List<Integer> model = new ArrayList<Integer>();
	for (Integer p : q) {
	    model.add(p);
	    List<Integer> val = new ArrayList<Integer>();
	    val.add(p);
	    allCombinaisons.add(val);
	    list_cmb.add(val);
	}
	for (int o = 2; o < model.size(); o++) {
	    list_cmb = calculInterCombinaisons(allCombinaisons, model, list_cmb,
		    o, model.size());
	}
	return allCombinaisons;
    }

}
