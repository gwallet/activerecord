package activerecord;

import activerecord.annotation.PrimaryKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.sql.DataSource;
import java.sql.SQLException;

@Data
@EqualsAndHashCode(callSuper = false)
public class Contact
    extends ActiveRecord<Contact>
{
    @PrimaryKey
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private Integer groupId;

    public ContactGroup getGroup()
        throws SQLException
    {
        ContactGroup sample = new ContactGroup();
        sample.setId( groupId );
        return sample.find().get( 0 );
    }
}
