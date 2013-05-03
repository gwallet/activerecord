package activerecord;

import org.dbunit.dataset.IDataSet;
import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class ReadTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canFindExactlyOneMatchingRecordInDB()
        throws Exception
    {
        Contact expectedContact = new Contact();
        expectedContact.setFirstName("Guillaume");
        expectedContact.setLastName("Wallet");
        expectedContact.setEmail("wallet.guillaume@gmail.com");
        List<Contact> contacts = expectedContact.find( dataSource );
        assertThat(contacts).hasSize(1);
        expectedContact.setId( 1 );
        assertThat(contacts.get(0)).isEqualTo( expectedContact );
    }

    @Test
    public void canFindByPrimaryKeyInDB()
        throws Exception
    {
        Contact expectedContact = new Contact();
        expectedContact.setId(1);
        List<Contact> contacts = expectedContact.find( dataSource );
        assertThat(contacts).hasSize(1);
        expectedContact.setFirstName( "Guillaume" );
        expectedContact.setLastName("Wallet");
        expectedContact.setEmail("wallet.guillaume@gmail.com");
        assertThat( contacts.get( 0 ) ).isEqualTo( expectedContact );
    }

    @Test
    public void canFindCorrespondingRecordsInDB()
        throws Exception
    {
        Contact expectedContact = new Contact();
        expectedContact.setFirstName("Guillaume");
        List<Contact> contacts = expectedContact.find( dataSource );
        assertThat(contacts).hasSize(3);
    }

    @Override
    protected IDataSet getDataSet()
        throws Exception
    {
        return loadFlatXmlDataSet("someContacts.xml");
    }
}
