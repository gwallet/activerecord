package activerecord;

import activerecord.annotation.PrimaryKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}
