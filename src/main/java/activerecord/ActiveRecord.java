package activerecord;

import activerecord.annotation.OneToMany;
import activerecord.annotation.PrimaryKey;
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
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Throwables.propagate;

/**
 * The main purpose of this class is to managed the database relationship of sub-classes instances.
 *
 * <p>Active record objects are instances of classes declared like this:</p>
 * <pre>
 * import activerecord.ActiveRecord;
 * import activerecord.annotation.PrimaryKey;
 *
 * public class Contact
 *     extends ActiveRecord&lt;Contact>
 * {
 *     {@literal @}PrimaryKey
 *     private Integer id;
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
 * The corresponding row never exist in database.
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
     * Save by inserting or updating this instance to the target database.
     * <pre>
     * Contact contact = new Contact();
     * // ...
     * // populate fields
     * // ...
     * contact.save( dataSource );
     * </pre>
     * The database is now up to date or contains a new row.
     * @param dataSource Previously configured target database connection.
     * @throws SQLException This may failed, sorry.
     */
    public void save( DataSource dataSource )
        throws SQLException
    {
        ArrayList<Object> args = new ArrayList<>();
        String query;
        if ( existInDatabase() ) {
            query = buildUpdateQuery( args );
        } else {
            query = buildInsertionQuery( args );
        }
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

    private boolean existInDatabase() {
        return hasPrimaryKeyNotNull();
    }

    private boolean hasPrimaryKeyNotNull() {
        for (Field field : getClass().getDeclaredFields()) {
            PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
            if (primaryKey != null) {
                field.setAccessible(true);
                try {
                    return field.get(this) != null;
                } catch ( IllegalAccessException ignored ) {}
            }
        }
        return false;
    }

    private String buildUpdateQuery(ArrayList<Object> args) {
        try {
            Query.UpdateQuery query = Query.update( getClass().getSimpleName() );
            String primaryKeyColumn = null;
            Object primaryKeyValue = null;
            for (Field field : getClass().getDeclaredFields()) {
                PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
                field.setAccessible(true);
                if (primaryKey != null) {
                    primaryKeyColumn = field.getName();
                    primaryKeyValue = field.get(this);
                } else {
                    args.add(field.get(this));
                    query.set( field.getName() , "?");
                }
            }
            args.add( primaryKeyValue );
            return query.where(primaryKeyColumn).isEqualTo( "?" ).toString();
        } catch ( IllegalAccessException ignored ) {}
        return null;
    }

    private String buildInsertionQuery( ArrayList<Object> args )
    {
        try {
            Query.InsertionQuery insert = Query.insertInto( getClass().getSimpleName() );
            for (Field field : getClass().getDeclaredFields()) {
                field.setAccessible(true);
                args.add(field.get(this));
                insert.column( field.getName() ).value("?");
            }
            return insert.toString();
        } catch ( IllegalAccessException ignored ) {}
        return null;
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
     * <pre>
     * Contact candidate = new Contact();
     * // ...
     * // populate matching fields
     * // ...
     * List&lt;Contact> contacts = candidate.find( dataSource );
     * </pre>
     * The list <code>contacts</code> contains all found contact in database corresponding to the given <code>candidate</code>.
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
                    createResultsFromResultSet( clazz, resultSet, results, dataSource );
                    return results;
                }
            }
        }
    }

    private <I> void createResultsFromResultSet( Class<I> clazz, ResultSet resultSet, ArrayList<I> results, DataSource dataSource )
        throws SQLException
    {
        while ( resultSet.next() ) {
            results.add( createResultFromRow( clazz, resultSet, dataSource ) );
        }
    }

    private <I> I createResultFromRow( Class<I> clazz, ResultSet resultSet, DataSource dataSource )
        throws SQLException
    {
        try {
            return createResultFromRowWithError( clazz, resultSet, dataSource );
        } catch ( IllegalAccessException|InstantiationException|NoSuchFieldException cause ) {
            throw new RuntimeException("Unable to execute selection query", cause);
        }
    }

    private <I> I createResultFromRowWithError( Class<I> clazz, ResultSet resultSet, DataSource dataSource )
        throws SQLException, IllegalAccessException, InstantiationException, NoSuchFieldException
    {
        I instance = clazz.newInstance();
        int index = 1;
        Object primaryKeyValue = null;
        for ( Field field : clazz.getDeclaredFields() ) {
            field.setAccessible( true );
            OneToMany oneToMany = field.getAnnotation(OneToMany.class);
            if ( oneToMany != null ) {
                final Class<? extends ActiveRecord> targetClazz = oneToMany.targetType();
                ActiveRecord sample = targetClazz.newInstance();
                Field targetForeignKey = targetClazz.getDeclaredField(oneToMany.targetForeignKey());
                targetForeignKey.setAccessible( true );
                targetForeignKey.set( sample, primaryKeyValue );
                field.set( instance, sample.find( dataSource ) );
            } else {
                Object value = resultSet.getObject( index++ );
                field.set( instance, value );
                if ( field.getAnnotation( PrimaryKey.class ) != null ) {
                    primaryKeyValue = value;
                }
            }
        }
        return instance;
    }

    private String buildSelectionQuery( ArrayList<Object> args )
    {
        try {
            Query.SelectionQuery select = null;
            Query.WhereQuery whereClause = null;
            for ( Field field : clazz.getDeclaredFields() ) {
                if ( field.getAnnotation( OneToMany.class ) != null ) {
                    continue;
                }
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
            return select.from(clazz.getSimpleName()).toString() + whereClause.toString();
        } catch (IllegalAccessException ignored) {}
        return null;
    }

    /**
     * Delete this instance from the target database.
     * <pre>
     * Contact candidate = new Contact();
     * // ...
     * // populate matching fields
     * // ...
     * candidate.delete( dataSource );
     * </pre>
     * The corresponding row(s) never exist(s) in database.
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
                logger().debug("Executing query '{}' with arguments {}", query, args);
                stopwatch.reset().start();
                statement.executeUpdate();
                stopwatch.stop();
                logger().info( "Executed query '{}' with arguments {} in {} ms", new Object []{query, args, stopwatch.elapsedMillis()});
            }
        }
    }

    private String buildDeletionQuery( ArrayList<Object> args )
    {
        try {
            Query.WhereQuery whereClause = null;
            for (Field field : clazz.getDeclaredFields()) {
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
            }
            return Query.delete().from(clazz.getSimpleName()) + whereClause.toString();
        } catch (IllegalAccessException ignored) {}
        return null;
    }
}
