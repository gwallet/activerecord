package activerecord;

import activerecord.annotation.OneToMany;
import activerecord.annotation.PrimaryKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class ContactGroup
    extends ActiveRecord<ContactGroup>
{
    @PrimaryKey
    private Integer id;
    private String name;
    @OneToMany(targetType = Contact.class, targetForeignKey = "groupId")
    private List<Contact> contacts;
}
