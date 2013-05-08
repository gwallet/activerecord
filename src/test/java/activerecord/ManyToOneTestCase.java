package activerecord;

import org.dbunit.dataset.IDataSet;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class ManyToOneTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canGetManyToOneRelationship() throws Exception {
        Contact sample = new Contact();
        sample.setId(1);
        Contact contact = sample.find( dataSource ).get(0);
        ContactGroup group = contact.getGroup( dataSource );
        assertThat(group.getId()).isEqualTo(1);
        assertThat(group.getName()).isEqualTo("mainGroup");
    }

    @Override
    protected IDataSet getDataSet() throws Exception {
        return loadFlatXmlDataSet("groupOfContacts.xml");
    }
}
