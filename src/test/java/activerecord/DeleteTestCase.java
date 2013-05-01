package activerecord;

import lombok.extern.slf4j.Slf4j;
import org.dbunit.database.DatabaseDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.dataset.xml.XmlDataSet;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;

import static org.dbunit.Assertion.assertEquals;

@Slf4j
public class DeleteTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canDelete()
        throws Exception
    {
        Contact contact = new Contact();
        contact.setFirstName( "Guillaume" );
        contact.setLastName( "Wallet" );
        contact.setEmail( "wallet.guillaume@gmail.com" );
        log.debug("Deleting contact {}", contact);
        contact.delete( dataSource );
        IDataSet actualDataSet = new DatabaseDataSet(getDatabaseConnection(), false);
        IDataSet expectedDataSet = new XmlDataSet(DeleteTestCase.class.getResourceAsStream("/emptyContact.xml"));
        assertEquals( expectedDataSet.getTable("contact"), actualDataSet.getTable("contact") );
        log.debug("Contact deleted.");
    }

    protected IDataSet getDataSet()
            throws Exception
    {
        return new FlatXmlDataSetBuilder().build(new File("src/test/resources/oneContact.xml"));
    }
}
