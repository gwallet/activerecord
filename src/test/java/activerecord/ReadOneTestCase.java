package activerecord;

import lombok.extern.slf4j.Slf4j;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.dbunit.Assertion.assertEquals;
import static org.fest.assertions.Assertions.assertThat;

@Slf4j
public class ReadOneTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canFindInDB()
        throws Exception
    {
        Contact expectedContact = new Contact();
        expectedContact.setFirstName( "Guillaume" );
        expectedContact.setLastName( "Wallet" );
        expectedContact.setEmail( "wallet.guillaume@gmail.com" );
        log.debug( "Reading contacts ..." );
        List<Contact> contacts = expectedContact.find( dataSource );
        log.debug( "{} contacts read", contacts.size() );
        assertThat(contacts).hasSize(1);
        assertThat( contacts.get( 0 ) ).isEqualTo( expectedContact );
    }

    @Override
    protected IDataSet getDataSet()
        throws Exception
    {
        return new FlatXmlDataSetBuilder().build(new File("src/test/resources/expectedContact.xml"));
    }
}
