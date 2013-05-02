package activerecord;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class CreateTestCase
    extends AbstractCRUDTestCase
{
    @Test
    public void canSaveIntoDB()
        throws Exception
    {
        Contact contact = new Contact();
        contact.setFirstName( "Guillaume" );
        contact.setLastName( "Wallet" );
        contact.setEmail( "wallet.guillaume@gmail.com" );
        log.debug("Saving contact {}", contact);
        contact.save(dataSource);
        expectTableContent("contact", "oneContact.xml");
        log.debug("Contact saved.");
    }
}
