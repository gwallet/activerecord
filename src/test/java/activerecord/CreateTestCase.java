package activerecord;

import org.junit.Test;

public class CreateTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canSaveIntoDB()
        throws Exception
    {
        Contact contact = new Contact();
        contact.setFirstName("Guillaume");
        contact.setLastName("Wallet");
        contact.setEmail("wallet.guillaume@gmail.com");
        contact.save();
        expectTableContent("contact", "oneContact.xml");
    }
}
