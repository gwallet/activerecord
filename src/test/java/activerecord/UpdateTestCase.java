package activerecord;

import org.dbunit.dataset.IDataSet;
import org.junit.Test;

public class UpdateTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canUpdateRecordInDB()
        throws Exception
    {
        Contact sample = new Contact();
        sample.setId( 1 );
        Contact contact = sample.find( dataSource ).get( 0 );
        contact.setEmail("guillaume.wallet@gmail.com");
        contact.save(dataSource);
        expectTableContent("contact", "anotherContact.xml");
    }

    @Override
    protected IDataSet getDataSet()
        throws Exception
    {
        return loadFlatXmlDataSet("oneContact.xml");
    }
}
