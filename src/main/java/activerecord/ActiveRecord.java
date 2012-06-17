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

public abstract class ActiveRecord<T extends ActiveRecord>
{
    @SuppressWarnings("unchecked")
    private Class<T> clazz = (Class<T>) getClass();

    private static final Stopwatch stopwatch = new Stopwatch();

    private Logger logger() {
        return LoggerFactory.getLogger(getClass());
    }

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
                    logger().info( "Executed query '{}' with values {} in {} ms", new Object []{query, args, stopwatch.elapsedMillis()});
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
            try {
                T instance = clazz.newInstance();
                int index = 1;
                for (Field field : clazz.getDeclaredFields() ) {
                    field.setAccessible(true);
                    field.set( instance, resultSet.getObject( index++ ) );
                }
                results.add( instance );
            } catch ( IllegalAccessException|InstantiationException cause ) {
                throw new RuntimeException("Unable to execute selection query", cause);
            }
        }
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

    public void delete( DataSource dataSource )
        throws SQLException
    {
        ArrayList<Object> args = new ArrayList<>();
        String query = buildDeletionQuery( args );
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                bindArguments(statement, args);
                stopwatch.reset().start();
                logger().debug( "Executing query '{}' with arguments {}", query, args );
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
