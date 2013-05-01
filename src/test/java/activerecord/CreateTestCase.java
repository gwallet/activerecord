package activerecord;

import lombok.extern.slf4j.Slf4j;
import org.dbunit.database.DatabaseDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Test;

import java.io.File;

import static org.dbunit.Assertion.assertEquals;

@Slf4j
public class CreateTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canSaveIntoDB()
        throws Exception
    {
        Contact contact = new Contact();
        contact.setFirstName( "Guillaume" );
        contact.setLastName( "Wallet" );
        contact.setEmail( "wallet.guillaume@gmail.com" );
        log.debug("Saving contact {}", contact);
        contact.save(dataSource);
        IDataSet actualDataSet = new DatabaseDataSet(getDatabaseConnection(), false);
        IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(new File("src/test/resources/oneContact.xml"));
        assertEquals( expectedDataSet.getTable("contact"), actualDataSet.getTable("contact") );
        log.debug("Contact saved.");
    }
}
