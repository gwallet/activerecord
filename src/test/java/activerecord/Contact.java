package activerecord;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Contact
    extends ActiveRecord<Contact>
{
    private String firstName;
    private String lastName;
    private String email;
}
