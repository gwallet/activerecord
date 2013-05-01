package activerecord;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The main purpose of this class is to managed the database relationship of sub-classes instances.
 *
 * <p>Active record objects are instances of classes declared like this:</p>
 * <pre>
 * public class Contact
 *     extends ActiveRecord&lt;Contact>
 * {
 *     private String firstName;
 *     private String lastName;
 *     private String email;
 * }
 * </pre>
 * <p>
 *     In this way, you can {@linkplain #save(javax.sql.DataSource) insert/update},
 *     {@linkplain #find(javax.sql.DataSource) find} and
 *     {@linkplain #delete(javax.sql.DataSource) delete} rows in database with the inherited methods.
 * </p>
 * <h2>Create / Update</h2>
 * To create or update, simply create an instance like anyone in Java, then store it in database like this :
 * <pre>
 * Contact contact = new Contact();
 * // ...
 * // populate fields
 * // ...
 * contact.save( dataSource );
 * </pre>
 * The database is now up to date or contains a new row.
 * <h2>Find</h2>
 * To find instances, simply create a candidate instance with some required field and then find equivalent in database like this :
 * <pre>
 * Contact candidate = new Contact();
 * // ...
 * // populate matching fields
 * // ...
 * List&lt;Contact> contacts = candidate.find( dataSource );
 * </pre>
 * The list <code>contacts</code> contains all found contact in database corresponding to the given <code>candidate</code>.
 * <h2>Delete</h2>
 * To delete a row, simply create a corresponding candidate and then delete it like this :
 * <pre>
 * Contact candidate = new Contact();
 * // ...
 * // populate matching fields
 * // ...
 * candidate.delete( dataSource );
 * </pre>
 *
 * @see <a href="http://en.wikipedia.org/wiki/Active_record_pattern">Active record design pattern</a>
 * @param <T> Type of managed active record.
 */
public abstract class ActiveRecord<T extends ActiveRecord>
{
    @SuppressWarnings("unchecked")
    private Class<T> clazz = (Class<T>) getClass();

    private static final Stopwatch stopwatch = new Stopwatch();

    private Logger logger() {
        return LoggerFactory.getLogger(getClass());
    }

    /**
     * Save this instance to the target database.
     * @param dataSource Previously configured target database connection.
     * @throws SQLException This may failed, sorry.
     */
    public void save( DataSource dataSource )
        throws SQLException
    {
        ArrayList<Object> args = new ArrayList<>();
        String query = buildInsertionQuery( args );
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement( query )) {
                bindArguments( statement, args );
                logger().debug("Executing query '{}' with values {}", query, args);
                stopwatch.reset().start();
                statement.executeUpdate();
                stopwatch.stop();
                logger().info( "Executed query '{}' with values {} in {} ms", new Object[] { query, args, stopwatch.elapsedMillis() } );
            }
        }
    }

    private String buildInsertionQuery( ArrayList<Object> args )
    {
        Query.InsertionQuery insert = Query.insertInto( getClass().getSimpleName() );
        try {
            for (Field field : getClass().getDeclaredFields()) {
                field.setAccessible(true);
                args.add(field.get(this));
                insert.column( field.getName() ).value("?");
            }
        } catch ( IllegalAccessException cause ) {
            throw new RuntimeException("Unable to build insertion query", cause);
        }
        return insert.toString();
    }

    private void bindArguments( PreparedStatement statement, ArrayList<Object> args )
            throws SQLException
    {
        int index = 1;
        for (Object arg : args) {
            statement.setObject(index++, arg);
        }
    }

    /**
     * Find all corresponding rows corresponding to this one in the target database.
     * @param dataSource Previously configured target database connection.
     * @return Return a list containing all the corresponding instance found in database. If no corresponding instance can
     * be found, an empty list is return.
     * @throws SQLException This may failed, sorry.
     */
    public List<T> find( DataSource dataSource )
        throws SQLException
    {
        ArrayList<Object> args = new ArrayList<>();
        String query = buildSelectionQuery( args );
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement( query )) {
                bindArguments( statement, args );
                logger().debug("Executing query '{}' with values {}", query, args);
                stopwatch.reset().start();
                try (ResultSet resultSet = statement.executeQuery()) {
                    stopwatch.stop();
                    logger().info( "Executed query '{}' with values {} in {} ms", new Object []{ query, args, stopwatch.elapsedMillis() } );
                    ArrayList<T> results = new ArrayList<>();
                    createResultsFromResultSet( resultSet, results );
                    return results;
                }
            }
        }
    }

    private void createResultsFromResultSet( ResultSet resultSet, ArrayList<T> results )
        throws SQLException
    {
        while ( resultSet.next() ) {
            results.add( createResultFromRow( resultSet ) );
        }
    }

    private T createResultFromRow( ResultSet resultSet )
        throws SQLException
    {
        try {
            return createResultFromRowWithError( resultSet );
        } catch ( IllegalAccessException|InstantiationException cause ) {
            throw new RuntimeException("Unable to execute selection query", cause);
        }
    }

    private T createResultFromRowWithError( ResultSet resultSet )
        throws SQLException, IllegalAccessException, InstantiationException
    {
        T instance = clazz.newInstance();
        int index = 1;
        for ( Field field : clazz.getDeclaredFields() ) {
            field.setAccessible( true );
            field.set( instance, resultSet.getObject( index++ ) );
        }
        return instance;
    }

    private String buildSelectionQuery( ArrayList<Object> args )
    {
        Query.SelectionQuery select = null;
        Query.WhereQuery whereClause = null;
        Field[] fields = clazz.getDeclaredFields();
        try {
            for (Field field : fields ) {
                field.setAccessible(true);
                Object arg = field.get( this );
                String fieldName = field.getName();
                if (select == null) {
                    select = Query.select(fieldName);
                } else {
                    select = select.and(fieldName);
                }
                if ( arg != null) {
                    if (whereClause == null) {
                        whereClause = Query.where( fieldName );
                    } else {
                        whereClause.and(fieldName);
                    }
                    whereClause.isEqualTo("?");
                    args.add(arg);
                }
            }
        } catch (IllegalAccessException cause) {
            throw new RuntimeException("Unable to build selection query", cause);
        }
        return select.from(clazz.getSimpleName()).toString() + whereClause.toString();
    }

    /**
     * Delete this instance from the target database.
     * @param dataSource Previously configured target database connection.
     * @throws SQLException This may failed, sorry.
     */
    public void delete( DataSource dataSource )
        throws SQLException
    {
        ArrayList<Object> args = new ArrayList<>();
        String query = buildDeletionQuery( args );
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                bindArguments(statement, args);
                logger().debug( "Executing query '{}' with arguments {}", query, args );
                stopwatch.reset().start();
                statement.executeUpdate();
                stopwatch.stop();
                logger().info( "Executed query '{}' with arguments {} in {} ms", new Object []{query, args, stopwatch.elapsedMillis()});
            }
        }
    }

    private String buildDeletionQuery( ArrayList<Object> args )
    {
        Query.WhereQuery whereClause = null;
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object arg = field.get(this);
                if ( arg != null) {
                    String fieldName = field.getName();
                    if (whereClause == null) {
                        whereClause = Query.where( fieldName );
                    } else {
                        whereClause.and(fieldName);
                    }
                    whereClause.isEqualTo("?");
                    args.add(arg);
                }
            } catch (IllegalAccessException cause) {
                throw new RuntimeException("Unable to build deletion query", cause);
            }
        }
        return Query.delete().from(clazz.getSimpleName()) + whereClause.toString();
    }
}
