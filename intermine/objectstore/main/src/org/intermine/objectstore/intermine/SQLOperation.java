package org.intermine.objectstore.intermine;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A functional interface (oh, please come Java 8 - you cannot come soon
 * enough) to help manage out-of-control boilerplate-itis in SQL code.
 * 
 * @author Alex Kalderimis
 *
 * @param <T> The return type of the operation.
 */
public abstract class SQLOperation<T> {

	/**
	 * The code that the operation represents.
	 * @param stm A prepared statement.
	 * @return A T (whatever that is).
	 * @throws SQLException whenever you even think of touching the statement.
	 */
	public abstract T run(PreparedStatement stm) throws SQLException;
}
