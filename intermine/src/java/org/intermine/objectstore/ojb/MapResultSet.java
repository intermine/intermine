/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.objectstore.ojb;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;

import java.sql.*;

/**
 * A map-backed ResultSet
 *
 * @author Mark Woodbridge
 * @author Andrew Varley
 */
public class MapResultSet implements ResultSet
{
    private Map row;
    private boolean lastGetWasNull = false;

    /**
     * @see java.sql.ResultSet
     */    
    public MapResultSet(Map row) {
        if (row == null) {
            throw new NullPointerException("Row cannot be null");
        }
        this.row = row;
    }

    /**
     * @throws SQLException
     * @see ResultSet#next
     */
    public boolean next() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#close
     */    
    public void close() throws SQLException {
    }

    /**
     * @throws SQLException
     * @see ResultSet#wasNull
     */    
    public boolean wasNull() throws SQLException {
        return lastGetWasNull;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getString
     */    
    public String getString(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBoolean
     */    
    public boolean getBoolean(int i) throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getByte
     */    
    public byte getByte(int i) throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getShort
     */    
    public short getShort(int i) throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getInt
     */    
    public int getInt(int i) throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getLong
     */    
    public long getLong(int i) throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getFloat
     */    
    public float getFloat(int i) throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getDouble
     */    
    public double getDouble(int i) throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBigDecimal
     * @deprecated Method getBigDecimal is deprecated
     */
    public BigDecimal getBigDecimal(int i, int j) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBytes
     */    
    public byte[] getBytes(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getDate
     */    
    public Date getDate(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getTime
     */    
    public Time getTime(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getTimestamp
     */    
    public Timestamp getTimestamp(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getAsciiStream
     */    
    public InputStream getAsciiStream(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getUnicodeStream
     * @deprecated Method getUnicodeStream is deprecated
     */
    public InputStream getUnicodeStream(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBinaryStream
     */    
    public InputStream getBinaryStream(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getString
     */    
    public String getString(String s) throws SQLException {
        return (String) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBoolean
     */    
    public boolean getBoolean(String s) throws SQLException {
        Boolean b = (Boolean) get(s);
        return (b == null ? false : b.booleanValue());
    }

    /**
     * @throws SQLException
     * @see ResultSet#getByte
     */    
    public byte getByte(String s) throws SQLException {
        Byte b = (Byte) get(s);
        return (b == null ? 0 : b.byteValue());
    }

    /**
     * @throws SQLException
     * @see ResultSet#getShort
     */    
    public short getShort(String s) throws SQLException {
        Short n = (Short) get(s);
        return (n == null ? 0 : n.shortValue());
    }

    /**
     * @throws SQLException
     * @see ResultSet#getInt
     */    
    public int getInt(String s) throws SQLException {
        Integer i = (Integer) get(s);
        return (i == null ? 0 : i.intValue());
    }

    /**
     * @throws SQLException
     * @see ResultSet#getLong
     */    
    public long getLong(String s) throws SQLException {
        Long l = (Long) get(s);
        return (l == null ? 0 : l.longValue());
    }

    /**
     * @throws SQLException
     * @see ResultSet#getFloat
     */    
    public float getFloat(String s) throws SQLException {
        Float f = (Float) get(s);
        return (f == null ? 0.0f : f.floatValue());
    }

    /**
     * @throws SQLException
     * @see ResultSet#getDouble
     */    
    public double getDouble(String s) throws SQLException {
        Double d = (Double) get(s);
        return (d == null ? 0.0 : d.doubleValue());
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBigDecimal
     * @deprecated Method getBigDecimal is deprecated
     */
    public BigDecimal getBigDecimal(String s, int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBytes
     */    
    public byte[] getBytes(String s) throws SQLException {
        return (byte[]) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getDate
     */    
    public Date getDate(String s) throws SQLException {
        return (Date) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getTime
     */    
    public Time getTime(String s) throws SQLException {
        return (Time) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getTimestamp
     */    
    public Timestamp getTimestamp(String s) throws SQLException {
        return (Timestamp) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getAsciiStream
     */    
    public InputStream getAsciiStream(String s) throws SQLException {
        return (InputStream) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getUnicodeStream
     * @deprecated Method getUnicodeStream is deprecated
     */
    public InputStream getUnicodeStream(String s) throws SQLException {
        return (InputStream) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBinaryStream
     */    
    public InputStream getBinaryStream(String s) throws SQLException {
        return (InputStream) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getWarnings
     */    
    public SQLWarning getWarnings() throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#clearWarnings
     */    
    public void clearWarnings() throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#getCursorName
     */    
    public String getCursorName() throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getMetaData
     */    
    public ResultSetMetaData getMetaData() throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getObject
     */    
    public Object getObject(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getObject
     */    
    public Object getObject(String s) throws SQLException {
        return get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#findColumn
     */    
    public int findColumn(String s) throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getCharacterStream
     */    
    public Reader getCharacterStream(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getCharacterStream
     */    
    public Reader getCharacterStream(String s) throws SQLException {
        return (Reader) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBigDecimal
     */    
    public BigDecimal getBigDecimal(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBigDecimal
     */    
    public BigDecimal getBigDecimal(String s) throws SQLException {
        return (BigDecimal) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#isBeforeFirst
     */    
    public boolean isBeforeFirst() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#isAfterLast
     */    
    public boolean isAfterLast() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#isFirst
     */    
    public boolean isFirst() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#isLast
     */    
    public boolean isLast() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#beforeFirst
     */    
    public void beforeFirst() throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#afterLast
     */    
    public void afterLast() throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#first
     */    
    public boolean first() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#last
     */    
    public boolean last() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getRow
     */    
    public int getRow() throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#absolute
     */    
    public boolean absolute(int i) throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#relative
     */    
    public boolean relative(int i) throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#previous
     */    
    public boolean previous() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#setFetchDirection
     */    
    public void setFetchDirection(int i) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#getFetchDirection
     */    
    public int getFetchDirection() throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#setFetchSize
     */    
    public void setFetchSize(int i) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#getFetchSize
     */    
    public int getFetchSize() throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getType
     */    
    public int getType() throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getConcurrency
     */    
    public int getConcurrency() throws SQLException {
        unsupported();
        return 0;
    }

    /**
     * @throws SQLException
     * @see ResultSet#rowUpdated
     */    
    public boolean rowUpdated() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#rowInserted
     */    
    public boolean rowInserted() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#rowDeleted
     */    
    public boolean rowDeleted() throws SQLException {
        unsupported();
        return false;
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateNull
     */    
    public void updateNull(int i) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateBoolean
     */    
    public void updateBoolean(int i, boolean flag) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateByte
     */    
    public void updateByte(int i, byte byte0) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateShort
     */    
    public void updateShort(int i, short word0) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateInt
     */    
    public void updateInt(int i, int j) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateLong
     */    
    public void updateLong(int i, long l) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateFloat
     */    
    public void updateFloat(int i, float f) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateDouble
     */    
    public void updateDouble(int i, double d) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateBigDecimal
     */    
    public void updateBigDecimal(int i, BigDecimal bigdecimal) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateString
     */    
    public void updateString(int i, String s) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateBytes
     */    
    public void updateBytes(int i, byte abyte0[]) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateDate
     */    
    public void updateDate(int i, Date date) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateTime
     */    
    public void updateTime(int i, Time time) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateTimestamp
     */    
    public void updateTimestamp(int i, Timestamp timestamp) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateAsciiStream
     */    
    public void updateAsciiStream(int i, InputStream inputstream, int j) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateBinaryStream
     */    
    public void updateBinaryStream(int i, InputStream inputstream, int j) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateCharacterStream
     */    
    public void updateCharacterStream(int i, Reader reader, int j) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateObject
     */    
    public void updateObject(int i, Object obj, int j) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateObject
     */    
    public void updateObject(int i, Object obj) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateNull
     */    
    public void updateNull(String s) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateBoolean
     */    
    public void updateBoolean(String s, boolean flag) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateByte
     */    
    public void updateByte(String s, byte byte0) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateShort
     */    
    public void updateShort(String s, short word0) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateInt
     */    
    public void updateInt(String s, int i) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateLong
     */    
    public void updateLong(String s, long l) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateFloat
     */    
    public void updateFloat(String s, float f) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateDouble
     */    
    public void updateDouble(String s, double d) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateBigDecimal
     */    
    public void updateBigDecimal(String s, BigDecimal bigdecimal) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateString
     */    
    public void updateString(String s, String s1) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateBytes
     */    
    public void updateBytes(String s, byte abyte0[]) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateDate
     */    
    public void updateDate(String s, Date date) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateTime
     */    
    public void updateTime(String s, Time time) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateTimestamp
     */    
    public void updateTimestamp(String s, Timestamp timestamp) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateAsciiStream
     */    
    public void updateAsciiStream(String s, InputStream inputstream, int i) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateBinaryStream
     */    
    public void updateBinaryStream(String s, InputStream inputstream, int i) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateCharacterStream
     */    
    public void updateCharacterStream(String s, Reader reader, int i) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateObject
     */    
    public void updateObject(String s, Object obj, int i) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateObject
     */    
    public void updateObject(String s, Object obj) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#insertRow
     */    
    public void insertRow() throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateRow
     */    
    public void updateRow() throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#deleteRow
     */    
    public void deleteRow() throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#refreshRow
     */    
    public void refreshRow() throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#cancelRowUpdates
     */    
    public void cancelRowUpdates() throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#moveToInsertRow
     */    
    public void moveToInsertRow() throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#moveToCurrentRow
     */    
    public void moveToCurrentRow() throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#getStatement
     */    
    public Statement getStatement() throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getObject
     */    
    public Object getObject(int i, Map map) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getRef
     */    
    public Ref getRef(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBlob
     */    
    public Blob getBlob(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getClob
     */    
    public Clob getClob(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getArray
     */    
    public Array getArray(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getObject
     */    
    public Object getObject(String s, Map map) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getRef
     */    
    public Ref getRef(String s) throws SQLException {
        return (Ref) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getBlob
     */    
    public Blob getBlob(String s) throws SQLException {
        return (Blob) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getClob
     */    
    public Clob getClob(String s) throws SQLException {
        return (Clob) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getArray
     */    
    public Array getArray(String s) throws SQLException {
        return (Array) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#getDate
     */    
    public Date getDate(int i, Calendar calendar) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getDate
     */    
    public Date getDate(String s, Calendar calendar) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getTime
     */    
    public Time getTime(int i, Calendar calendar) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getTime
     */    
    public Time getTime(String s, Calendar calendar) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getTimestamp
     */    
    public Timestamp getTimestamp(int i, Calendar calendar) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getTimestamp
     */    
    public Timestamp getTimestamp(String s, Calendar calendar) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getURL
     */    
    public URL getURL(int i) throws SQLException {
        unsupported();
        return null;
    }

    /**
     * @throws SQLException
     * @see ResultSet#getURL
     */    
    public URL getURL(String s) throws SQLException {
        return (URL) get(s);
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateRef
     */    
    public void updateRef(int i, Ref ref) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateRef
     */    
    public void updateRef(String s, Ref ref) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateBlob
     */    
    public void updateBlob(int i, Blob blob) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateBlob
     */    
    public void updateBlob(String s, Blob blob) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateClob
     */    
    public void updateClob(int i, Clob clob) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateClob
     */    
    public void updateClob(String s, Clob clob) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateArray
     */    
    public void updateArray(int i, Array array) throws SQLException {
        unsupported();
    }

    /**
     * @throws SQLException
     * @see ResultSet#updateArray
     */    
    public void updateArray(String s, Array array) throws SQLException {
        unsupported();
    }

    private Object get(String s) throws SQLException {
        Object retval = null;
        if (row.containsKey(s)) {
            retval = row.get(s);
        } else if (row.containsKey(s.toLowerCase())) {
            retval = row.get(s.toLowerCase());
        } else {
            throw new SQLException("Column " + s + " not found");
        }
        lastGetWasNull = (retval == null);
        return retval;
    }
    
    private void unsupported() throws SQLException {
        throw new SQLException("Operation not supported");
    }

    /**
     * Returns the String representation of the map that backs this ResultSet
     * @return a String reprentation of the map
     */
    public String toString() {
        return row.toString();
    }
}
