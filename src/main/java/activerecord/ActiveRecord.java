package activerecord;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
public abstract class ActiveRecord<T extends ActiveRecord>
{
    @Nonnull
    @SuppressWarnings("unchecked")
    private Class<T> clazz = (Class<T>) getClass();

    public void save( DataSource dataSource )
        throws SQLException
    {
        try (Connection connection = dataSource.getConnection()) {
            String query = buildQuery();
            try (PreparedStatement statement = connection.prepareStatement( query )) {
                Object[] args = populate(statement);
                log.debug("Execute query '{}' with values {}", query, args);
                statement.executeUpdate();
            }
        }
    }

    private String buildQuery()
    {
        InsertQueryBuilder insertQueryBuilder = new InsertQueryBuilder().insertInto( getClass().getSimpleName() );
        for (Field field : getClass().getDeclaredFields()) {
            insertQueryBuilder.addColumn( field.getName() );
        }
        return insertQueryBuilder.build();
    }

    private Object[] populate( PreparedStatement statement )
        throws SQLException
    {
        int index = 0;
        Object[] args;
        try {
            Field[] fields = getClass().getDeclaredFields();
            args = new Object[fields.length];
            for ( Field field : fields ) {
                field.setAccessible(true);
                args[index++] = field.get(this);
                statement.setObject(index, args[index - 1]);
            }
        } catch ( IllegalAccessException cause ) {
            throw new RuntimeException("Unable to populate query :'(", cause);
        }
        return args;
    }

    public <T> List<T> findCorresponding( DataSource dataSource )
        throws SQLException
    {
        try {
            SelectQueryBuilder queryBuilder = new SelectQueryBuilder().selectFrom( clazz );
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields ) {
                field.setAccessible(true);
                Object arg = field.get( this );
                if ( arg != null) {
                    queryBuilder.where( field.getName() );
                }
            }
            String query = queryBuilder.build();
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement preparedStatement = connection.prepareStatement( query )) {
                    int index = 0;
                    Object[] args = new Object[fields.length];
                    for (Field field : fields ) {
                        field.setAccessible(true);
                        args[index++] = field.get( this );
                        preparedStatement.setObject(index, args[index - 1] );
                    }
                    log.debug("Execute query '{}' with values {}", query, args);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        ArrayList<T> results = new ArrayList<>();
                        while (resultSet.next()) {
                            @SuppressWarnings("unchecked")
                            T instance = (T) clazz.newInstance();
                            index = 1;
                            for (Field field : fields ) {
                                field.setAccessible(true);
                                field.set( instance, resultSet.getObject(index++ ) );
                            }
                            results.add(instance);
                        }
                        return results;
                    }
                }
            }
        } catch (IllegalAccessException|InstantiationException cause) {
            throw new RuntimeException("Unable to execute select query", cause);
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
