package activerecord;

import org.dbunit.dataset.IDataSet;
import org.junit.Test;

public class DeleteTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canDelete()
        throws Exception
    {
        Contact contact = new Contact();
        contact.setFirstName("Guillaume");
        contact.setLastName("Wallet");
        contact.setEmail("wallet.guillaume@gmail.com");
        contact.delete(dataSource);
        expectTableContent("contact", loadXmlDataSet("emptyContact.xml"));
    }

    protected IDataSet getDataSet()
            throws Exception
    {
        return loadFlatXmlDataSet("oneContact.xml");
    }
}
