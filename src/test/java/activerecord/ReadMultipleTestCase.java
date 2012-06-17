package activerecord;

import lombok.extern.slf4j.Slf4j;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

@Slf4j
public class ReadMultipleTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canFindInDB()
        throws Exception
    {
        Contact expectedContact = new Contact();
        expectedContact.setFirstName( "Guillaume" );
        log.debug( "Reading contacts ..." );
        List<Contact> contacts = expectedContact.find( dataSource );
        log.debug( "{} contacts read : {}", contacts.size(), contacts );
        assertThat(contacts).hasSize(3);
    }

    @Override
    protected IDataSet getDataSet()
        throws Exception
    {
        return new FlatXmlDataSetBuilder().build(new File("src/test/resources/someContacts.xml"));
    }
}
