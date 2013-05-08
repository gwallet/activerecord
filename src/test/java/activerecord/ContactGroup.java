package activerecord;

import activerecord.annotation.PrimaryKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class ContactGroup
    extends ActiveRecord<ContactGroup>
{
    @PrimaryKey
    private Integer id;
    private String name;

    public List<Contact> getContacts( DataSource dataSource )
        throws SQLException
    {
        Contact sample = new Contact();
        sample.setGroupId( id );
        return sample.find( dataSource );
    }
}
