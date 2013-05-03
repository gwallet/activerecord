package activerecord;

import org.dbunit.dataset.IDataSet;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class OneToManyTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canListOneToManyRelationship() throws Exception {
        ContactGroup sample = new ContactGroup();
        sample.setId(1);
        ContactGroup group = sample.find( dataSource ).get(0);
        assertThat(group.getContacts()).hasSize(3);
    }

    @Override
    protected IDataSet getDataSet() throws Exception {
        return loadFlatXmlDataSet("groupOfContacts.xml");
    }
}
