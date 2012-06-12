package activerecord;

import com.googlecode.flyway.core.Flyway;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.DatabaseDataSet;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.dbunit.Assertion.assertEquals;

@Slf4j
public class SaveTestCase
{
    private static DataSource dataSource;

    @BeforeClass
    public static void setUpClass()
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
        log.debug( "Database ready." );
    }

    @Test
    public void canSaveIntoDB()
        throws Exception
    {
        Contact contact = new Contact();
        contact.setFirstName( "Guillaume" );
        contact.setLastName("Wallet");
        contact.setEmail("wallet.guillaume@gmail.com");
        log.debug("Saving contact {}", contact);
        contact.save(dataSource);
        IDataSet actualDataSet = new DatabaseDataSet(getDatabaseConnection(), false);
        IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(new File("src/test/resources/expectedContact.xml"));
        assertEquals( expectedDataSet.getTable("contact"), actualDataSet.getTable("contact") );
        log.debug("Contact saved.");
    }

    private IDatabaseConnection getDatabaseConnection()
        throws SQLException, DatabaseUnitException
    {
        return new DatabaseConnection(dataSource.getConnection());
    }

    @Data
    static class Contact
        extends ActiveRecord
    {
        private String firstName;
        private String lastName;
        private String email;
    }
}
