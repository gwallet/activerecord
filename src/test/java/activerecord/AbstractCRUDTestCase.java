package activerecord;

import com.google.common.io.Resources;
import com.googlecode.flyway.core.Flyway;
import lombok.extern.slf4j.Slf4j;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseDataSet;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

import static org.dbunit.Assertion.assertEquals;

@Slf4j
public abstract class AbstractCRUDTestCase
{
    protected DataSource dataSource;

    protected IDataSet actualDataSet;

    @Before
    public void setUpDatabase()
        throws Exception
    {
        log.debug("Opening database ...");
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource = h2DataSource;
        log.debug("Creating database ...");
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
        if (getDataSet() != null) {
            log.debug("Populating database ...");
            DatabaseOperation.CLEAN_INSERT.execute(getDatabaseConnection(), getDataSet());
        }
        actualDataSet = new DatabaseDataSet(getDatabaseConnection(), false);
        log.debug( "Database ready." );
    }

    @After
    public void tearDownDatabase()
        throws Exception
    {
        if (getDataSet() != null) {
            log.debug("Shutting down database ...");
            DatabaseOperation.DELETE_ALL.execute(getDatabaseConnection(), getDataSet());
        }
    }

    protected IDataSet getDataSet()
        throws Exception
    {
        return null;
    }

    protected IDatabaseConnection getDatabaseConnection()
        throws SQLException, DatabaseUnitException
    {
        return new DatabaseConnection(dataSource.getConnection());
    }

    protected void expectTableContent(String tableName, String resourceName)
        throws SQLException, DatabaseUnitException
    {
        expectTableContent(tableName, loadFlatXmlDataSet(resourceName));
    }

    protected void expectTableContent(String tableName, IDataSet expectedDataSet)
        throws SQLException, DatabaseUnitException
    {
        assertEquals(expectedDataSet.getTable(tableName), actualDataSet.getTable(tableName));
    }

    protected XmlDataSet loadXmlDataSet(String resourceName) throws DataSetException, IOException {
        return new XmlDataSet(Resources.getResource(resourceName).openStream());
    }

    protected IDataSet loadFlatXmlDataSet(String resourceName) throws DataSetException {
        return new FlatXmlDataSetBuilder().build(Resources.getResource(resourceName));
    }
}
