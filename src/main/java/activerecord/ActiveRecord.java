package activerecord;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;

public abstract class ActiveRecord<T extends ActiveRecord>
{
    @Nonnull
    @SuppressWarnings("unchecked")
    private Class<T> clazz = (Class<T>) getClass();

    private static final Stopwatch stopwatch = new Stopwatch();

    private Logger logger() {
        return LoggerFactory.getLogger(getClass());
    }

    public void save( DataSource dataSource )
        throws SQLException
    {
        try (Connection connection = dataSource.getConnection()) {
            InsertQueryBuilder insertQueryBuilder = new InsertQueryBuilder().insertInto( getClass().getSimpleName() );
            ArrayList<Object> args = new ArrayList<>();
            try {
                for (Field field : getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    args.add(field.get(this));
                    insertQueryBuilder.addColumn( field.getName() );
                }
            } catch ( IllegalAccessException cause ) {
                throw new RuntimeException("Unable to build insertion query", cause);
            }
            String query =  insertQueryBuilder.build();
            try (PreparedStatement statement = connection.prepareStatement( query )) {
                int index = 1;
                for (Object arg : args) {
                    statement.setObject(index++, arg);
                }
                logger().debug("Executing query '{}' with values {}", query, args);
                stopwatch.reset().start();
                statement.executeUpdate();
                stopwatch.stop();
                logger().info( "Executed query '{}' with values {} in {} ms", new Object[] { query, args, stopwatch.elapsedMillis() } );
            }
        }
    }

    public List<T> find( DataSource dataSource )
        throws SQLException
    {
        SelectQueryBuilder queryBuilder = new SelectQueryBuilder().selectFrom( clazz );
        Field[] fields = clazz.getDeclaredFields();
        ArrayList<Object> args = new ArrayList<>();
        try {
            for (Field field : fields ) {
                field.setAccessible(true);
                Object arg = field.get( this );
                if ( arg != null) {
                    queryBuilder.where( field.getName() );
                    args.add(arg);
                }
            }
        } catch (IllegalAccessException cause) {
            throw new RuntimeException("Unable to build selection query", cause);
        }
        String query = queryBuilder.build();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement( query )) {
                int index = 1;
                for (Object arg : args ) {
                    preparedStatement.setObject(index++, arg );
                }
                logger().debug("Executing query '{}' with values {}", query, args);
                stopwatch.reset().start();
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    stopwatch.stop();
                    logger().info( "Executed query '{}' with values {} in {} ms", new Object []{query, args, stopwatch.elapsedMillis()});
                    ArrayList<T> results = new ArrayList<>();
                    while (resultSet.next()) {
                        try {
                            T instance = clazz.newInstance();
                            index = 1;
                            for (Field field : fields ) {
                                field.setAccessible(true);
                                field.set( instance, resultSet.getObject(index++ ) );
                            }
                            results.add(instance);
                        } catch (IllegalAccessException|InstantiationException cause) {
                            throw new RuntimeException("Unable to execute selection query", cause);
                        }
                    }
                    return results;
                }
            }
        }
    }

    private class InsertQueryBuilder
    {
        private String tableName;
        private List<String> columns = new ArrayList<>();

        public InsertQueryBuilder insertInto( String tableName )
        {
            this.tableName = tableName;
            return this;
        }

        public InsertQueryBuilder addColumn( String columnName )
        {
            columns.add(columnName);
            return this;
        }

        public String build()
        {
            StringBuilder buffer = new StringBuilder("INSERT INTO ")
                    .append(tableName)
                    .append( " (" );
            Joiner joiner = Joiner.on(", ");
            buffer.append(joiner.join(columns));
            buffer.append(") VALUES (");
            buffer.append(joiner.join(transform(columns, new Function<String, String>() {
                @Override
                public String apply( @Nullable String input )
                {
                    return "?";
                }
            })));
            buffer.append(")");
            return buffer.toString();
        }
    }

    private class SelectQueryBuilder
    {
        private String tableName;
        private String selection;
        private StringBuilder whereClause;

        public SelectQueryBuilder selectFrom( Class aClass )
        {
            tableName = aClass.getSimpleName();
            Iterable<String> names = transform( asList(aClass.getDeclaredFields()), new Function<Field, String>() {
                @Override
                public String apply( @Nullable Field input )
                {
                    return input == null ? null : input.getName();
                }
            } );
            Joiner joiner = Joiner.on(", ");
            selection = "SELECT " + joiner.join(names);
            return this;
        }

        public SelectQueryBuilder where( String columnName )
        {
            if (whereClause == null) {
                whereClause = new StringBuilder(" WHERE ");
            } else {
                whereClause.append(" AND ");
            }
            whereClause.append(columnName).append(" = ? ");
            return this;
        }

        public String build()
        {
            return selection + " FROM " + tableName + whereClause;
        }
    }
}
