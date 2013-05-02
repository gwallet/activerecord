package activerecord;

import lombok.extern.slf4j.Slf4j;
import org.dbunit.dataset.IDataSet;
import org.junit.Test;

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
        expectTableContent("contact", loadXmlDataSet("emptyContact.xml"));
        log.debug("Contact deleted.");
    }

    protected IDataSet getDataSet()
            throws Exception
    {
        return loadFlatXmlDataSet("oneContact.xml");
    }
}
