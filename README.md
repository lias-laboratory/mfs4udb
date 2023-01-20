# MFS4UDB

MFS4UDB (Minimal Failing Subqueries 4 Uncertain DataBases) deals with the empty-answer problem. A large number of applications manage uncertain data and usually, users expect high-quality results when they pose queries with strict conditions over these data. However, their queries may be failing as they may not be clear about the contents of the database (i.e., they may return no result or results that do not satisfy the expected degree of certainty).

In this project, we deal with this problem in the field of uncertain databases, by proposing efficient approaches that identify a set of subqueries called Minimal Failing Subqueries (MFSs) and maXimal Succeeding Subqueries (XSSs). We propose two new approaches: [MBS](http://ieeexplore.ieee.org/document/8015476/) and MDMB to compute such cooperative answers and compare them to some related work alternatives.

## Software requirements

* Java version >= 8.
* PostgreSQL >= 9.x.
* Maven.
* Linux OS (if you want to generate dataset yourself).
* Sparse Hypergraph Dualization Algorithm [SHD](http://research.nii.ac.jp/~uno/code/shd.html) (for the use of MDMB approach). Download [here](http://research.nii.ac.jp/~uno/code/shd31.zip).

## Compilation

* Compile the project and deploy the artifcats to the local Maven repository.

```
$ mvn clean install
```

## Prepare and insert dataset into PostgreSQL

You can start the execution by using the *ready to use* dataset (100 rows) or by generating a new dataset.

### From a ready to use dataset

* Import _deploy/mfs4udb-dataset.sql_ dump (_deploy/mfs4udb-dataset.sql_ is a *ready to use* dataset with 100 rows) into PostgreSQL database. *Note:* `$USER_POSTGRES` must be replaced by your PostgreSQL user.

```console
$ psql -U $USER_POSTGRES postgres < deploy/mfs4udb-dataset.sql
```

### From scratch 

* Install randdataset tool from _install*randdataset.sh_ script (a bash script to install Random dataset generator for SKYLINE operator evaluation. [Find it here](http://pgfoundry.org/projects/randdataset/)).

```console
$ sudo ./deploy/install-randdataset.sh
```

* Create dataset by using the following command (100 is the number of rows generated, `$USER_POSTGRES` is your PostgreSQL user and `$PASSWORD_POSTGRES` your PostgreSQL password). _./deploy/mfs4udb-generate.sh_ is a bash script to generate a new dataset.

```
$ ./deploy/mfs4udb-generate.sh 100 $USER_POSTGRES $PASSWORD_POSTGRES
```

### From a real dataset

For reproducibility of the experiments, we supply three datasets (Weather, NBA and House) obtained from the [SkyBench GitHub repository](https://github.com/sean-chester/SkyBench).

* Download the datasets

```
$ wget https://raw.githubusercontent.com/sean-chester/SkyBench/master/workloads/elv_weather-U-15-566268.csv
$ wget https://raw.githubusercontent.com/sean-chester/SkyBench/master/workloads/house-U-6-127931.csv
$ wget https://raw.githubusercontent.com/sean-chester/SkyBench/master/workloads/nba-U-8-17264.csv
```

* Import _deploy/mfs4udb-realdataset.sql_ SQL script into PostgreSQL database. *Note:* `$USER_POSTGRES` must be replaced by your PostgreSQL user.

```
$ psql -U $USER_POSTGRES postgres < mfs4udb-realdataset.sql
```

## Execute the algorithms

### Step 1: classpath Initialization instructions

* Create a Maven Java project from your favorite IDE.

* Add Maven dependency

```xml
<groupId>fr.ensma.lias</groupId>
<artifactId>mfs4udb</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

### Step 2: create class instructions

* Create a Java class and copy the following content. 

```java
public class MFS4UDBSample
  public static void main(String[] args) throws Exception {
    Class.forName("org.postgresql.Driver");
    Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5434/postgres", <$USER_POSTGRES>, <$PASSWORD_POSTGRES>);
	
    char[] chars = { 'a', 'c', 'i' };
    for (int i = 0; i < chars.length; i++) {
      BufferedWriter fichier = new BufferedWriter(
      new FileWriter("exp1-" + chars[i] + ".csv"));
      Query q = new Query("P1 < 0.1 AND P2 < 0.1 AND P3 < 0.1 AND P4 < 0.1", "lasttab" + chars[i], c);
      List<Query> foundMFS = q.$MFS4UDB_ALGO
      fichier.write(foundMFS.size() + "\n");
      fichier.close();
    }
  }
}
```

* Adapt the content to replace `<$USER_POSTGRES>` by your PostgreSQL user and `<$PASSWORD_POSTGRES>` by your PostgreSQL password.

* For `$MFS4UDB_ALGO` select the algorithm that you want to run (`degree` is the expected degree of certainty and `useMatrix` specifies whether the matrix optimization should be used):

  * `getAllMFSWithDFS(double degree, boolean useMatrix)`: a depth-first search algorithm for traversing the subquery lattice for uncertain databases.

  * `getAllMFSWithLBA(double degree, boolean useMatrix)`: the algorithm published in [1] adapted for uncertain databases. 

  * `getAllMFSWithMCS(double degree, boolean useMatrix)`: the algorithm published in [2] adapted for uncertain databases.

  * `getAllMFSWithMBS(double degree)`: our proposed algorithm MBS.

  * `getAllMFSWithMDMB(double degree, int indice (default 1), String SHD_PATH)`: our proposed algorithm MDMB. `SHD_PATH` is the filesystem path where SHD tool is installed. 

1. Fokou, G., Jean, S., Hadjali, A., Baron, M.: Handling Failing RDF Queries: From Diagnosis to Relaxation. KAIS 2016.
2. McSherry, D.: Incremental Relaxation of Unsuccessful Queries. In: Advances in Case-Based Reasoning. 2004 131–148.

* You can change the query. Default value is: `P1 < 0.1 AND P2 < 0.1 AND P3 < 0.1 AND P4 < 0.1`

### Step 3: results

* All results are supplied into three files: _exp1-a.csv_, _exp1-c.csv_ and _exp1-i.csv_.

## Publications

* Chourouk Belheouane, Stéphane Jean, Allel Hadjali, Hamid Azzoune: [Handling failing queries over uncertain databases. FUZZ-IEEE 2017: 1-6](http://ieeexplore.ieee.org/document/8015476/)

## Software licence agreement

Details the license agreement of OntoQLPlus V1: [LICENCE](LICENCE)

## Historic Contributors

* [Chourouk BELHEOUANE(core developer)](https://www.lias-lab.fr/members/chouroukbelheouane/)
* [Stéphane JEAN](https://www.lias-lab.fr/members/stephanejean/)
* [Brice CHARDIN](https://www.lias-lab.fr/members/bricechardin/)
* [Mickael BARON](https://www.lias-lab.fr/members/mickaelbaron/)
* [Allel HADJ ALI](https://www.lias-lab.fr/members/allelhadjali/)

## Code Analysis

* Lines of Code: 1500
* Programming Languages: Java and shell Bash
