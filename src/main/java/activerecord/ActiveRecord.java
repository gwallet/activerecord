package activerecord;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public abstract class ActiveRecord
{
    public void save( DataSource dataSource )
        throws SQLException
    {
        try (Connection connection = dataSource.getConnection()) {
            String query = buildQuery();
            try (PreparedStatement statement = connection.prepareStatement( query )) {
                Object[] args = populate(statement);
                log.debug("Executing query : \"{}\" with values {}", query, args);
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
            log.error("Unable to populate query :'(", cause);
            throw new RuntimeException(cause);
        }
        return args;
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
            StringBuilder buffer = new StringBuilder();
            int columnCount = 0;
            buffer.append("INSERT INTO ");
            buffer.append(tableName);
            buffer.append(" (");
            for (String column : columns) {
                buffer.append(column);
                columnCount++;
                buffer.append(",");
            }
            buffer.append(") VALUES (");
            while (0 < columnCount--) {
                buffer.append("?,");
            }
            buffer.append(")");
            return buffer.toString();
        }
    }
}
