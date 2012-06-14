package activerecord;

import com.googlecode.flyway.core.Flyway;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Slf4j
public abstract class AbstractCRUDTestCase
{
    protected DataSource dataSource;

    @Data
    @EqualsAndHashCode(callSuper = false)
    static class Contact
        extends ActiveRecord<Contact>
    {
        private String firstName;
        private String lastName;
        private String email;
    }

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
}
