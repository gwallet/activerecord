package activerecord;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class QueryTestCase
{
    @Test
    public void canBuildInsertOrder() {
        String actual = Query
                .insertInto("Contact")
                .column("firstName").value("?")
                .column("lastName").value("?")
                .column("email").value("?")
                .toString();
        String expected = "INSERT INTO Contact (firstName, lastName, email) VALUES (?, ?, ?)";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void canBuildSelectOrder() {
        String actual = Query
                .select("firstName")
                .and("lastName")
                .and("email")
                .from("Contact")
                .where("firstName").isEqualTo("?")
                .and("lastName").isEqualTo("?")
                .and("email").isEqualTo("?")
                .toString();
        String expected = "SELECT firstName, lastName, email FROM Contact WHERE firstName = ? AND lastName = ? AND email = ?";
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void canBuildDeleteOrder() {
        String actual = Query.delete()
                .from("Contact")
                .where("firstName").isEqualTo("?")
                .and("lastName").isEqualTo("?")
                .and("email").isEqualTo("?")
                .toString();
        String expected = "DELETE FROM Contact WHERE firstName = ? AND lastName = ? AND email = ?";
        assertThat(actual).isEqualTo(expected);
    }
}
