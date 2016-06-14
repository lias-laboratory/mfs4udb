package fr.ensma.lias.mfs4udb.cfg;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.Sources;

/**
 * @author Mickael BARON
 */
@Sources("classpath:mfs4udb.config")
public interface MFS4UDBConfig extends Config {

    @Key("oracle.url")
    String oracleUrl();

    @Key("oracle.driver")
    String oracleDriver();

    @Key("oracle.login")
    String oracleLogin();

    @Key("oracle.password")
    String oraclePassword();

    @Key("mysql.url")
    String mysqlUrl();

    @Key("mysql.driver")
    String mysqlDriver();

    @Key("mysql.login")
    String mysqlLogin();

    @Key("mysql.password")
    String mysqlPassword();
    
    @Key("experimental.database.type")
    String experimentalDatabaseType();
    
    @Key("experimental.queries.input.filename")
    String experimentalQueriesInputFilename();

    @Key("experimental.occurence.number")
    String experimentalOccurenceNumber();
}
