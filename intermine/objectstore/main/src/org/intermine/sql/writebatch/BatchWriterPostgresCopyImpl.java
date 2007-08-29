package org.intermine.sql.writebatch;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.util.SensibleByteArrayOutputStream;
import org.intermine.util.StringConstructor;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.apache.log4j.Logger;

/**
 * An implementation of the BatchWriter interface that uses PostgreSQL-specific COPY commands.
 *
 * @author Matthew Wakeling
 */
public class BatchWriterPostgresCopyImpl extends BatchWriterPreparedStatementImpl
{
    private static final Logger LOG = Logger.getLogger(BatchWriterPostgresCopyImpl.class);
    protected static final BigInteger TEN = new BigInteger("10");
    protected static final BigInteger HUNDRED = new BigInteger("100");
    protected static final BigInteger THOUSAND = new BigInteger("1000");
    protected static final BigInteger TEN_THOUSAND = new BigInteger("10000");

    /**
     * {@inheritDoc}
     */
    protected int doInserts(String name, TableBatch table, List batches) throws SQLException {
        String colNames[] = table.getColNames();
        if ((colNames != null) && (!table.getIdsToInsert().isEmpty())) {
            try {
                CopyManager copyManager = null;
                if (con instanceof PGConnection) {
                    copyManager = ((PGConnection) con).getCopyAPI();
                }
                if (copyManager == null) {
                    LOG.warn("Database with Connection " + con.getClass().getName()
                            + " is incompatible with the PostgreSQL COPY command - falling"
                            + " back to prepared statements");
                    super.doInserts(name, table, batches);
                } else {
                    SensibleByteArrayOutputStream baos = new SensibleByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeBytes("PGCOPY\n");
                    dos.writeByte(255);
                    dos.writeBytes("\r\n");
                    dos.writeByte(0); // Signature done
                    dos.writeInt(0); // Flags - we aren't supplying OIDS
                    dos.writeInt(0); // Length of header extension
                    Iterator insertIter = table.getIdsToInsert().entrySet().iterator();
                    while (insertIter.hasNext()) {
                        Map.Entry insertEntry = (Map.Entry) insertIter.next();
                        Object inserts = insertEntry.getValue();
                        if (inserts instanceof Object[]) {
                            Object values[] = (Object[]) inserts;
                            dos.writeShort(colNames.length);
                            for (int i = 0; i < colNames.length; i++) {
                                writeObject(dos, values[i]);
                            }
                        } else {
                            Iterator iter = ((List) inserts).iterator();
                            while (iter.hasNext()) {
                                Object values[] = (Object[]) iter.next();
                                dos.writeShort(colNames.length);
                                for (int i = 0; i < colNames.length; i++) {
                                    writeObject(dos, values[i]);
                                }
                            }
                        }
                    }
                    StringBuffer sqlBuffer = new StringBuffer("COPY ").append(name).append(" (");
                    for (int i = 0; i < colNames.length; i++) {
                        if (i > 0) {
                            sqlBuffer.append(", ");
                        }
                        sqlBuffer.append(colNames[i]);
                    }
                    sqlBuffer.append(") FROM STDIN BINARY");
                    String sql = sqlBuffer.toString();
                    dos.writeShort(-1);
                    dos.flush();
                    batches.add(new FlushJobPostgresCopyImpl(copyManager, sql,
                                baos.toByteArray()));
                }
            } catch (IOException e) {
                throw new SQLException(e.toString());
            }
            return table.getIdsToInsert().size();
        }
        return 0;
    }

    private static void writeObject(DataOutputStream dos, Object o) throws IOException {
        if (o == null) {
            dos.writeInt(-1);
        } else if (o instanceof Integer) {
            dos.writeInt(4);
            dos.writeInt(((Integer) o).intValue());
        } else if (o instanceof Short) {
            dos.writeInt(2);
            dos.writeShort(((Short) o).intValue());
        } else if (o instanceof Boolean) {
            dos.writeInt(1);
            dos.writeByte(((Boolean) o).booleanValue() ? 1 : 0);
        } else if (o instanceof Float) {
            dos.writeInt(4);
            dos.writeFloat(((Float) o).floatValue());
        } else if (o instanceof Double) {
            dos.writeInt(8);
            dos.writeDouble(((Double) o).doubleValue());
        } else if (o instanceof Long) {
            dos.writeInt(8);
            dos.writeLong(((Long) o).longValue());
        } else if (o instanceof String) {
            byte bytes[] = ((String) o).getBytes("UTF-8");
            dos.writeInt(bytes.length);
            dos.write(bytes);
        } else if (o instanceof StringConstructor) {
            ArrayList<byte []> byteArrays = new ArrayList();
            int totalLength = 0;
            for (String string : ((StringConstructor) o).getStrings()) {
                byte bytes[] = ((String) string).getBytes("UTF-8");
                totalLength += bytes.length;
                byteArrays.add(bytes);
            }
            dos.writeInt(totalLength);
            for (byte bytes[] : byteArrays) {
                dos.write(bytes);
            }
        } else if (o instanceof BigDecimal) {
            BigInteger unscaledValue = ((BigDecimal) o).unscaledValue();
            int signum = ((BigDecimal) o).signum();
            if (signum == -1) {
                unscaledValue = unscaledValue.negate();
            }
            int scale = ((BigDecimal) o).scale();
            int nBaseScale = (scale + 3) / 4;
            int nBaseScaleRemainder = scale % 4;
            List digits = new ArrayList();
            if (nBaseScaleRemainder == 1) {
                BigInteger res[] = unscaledValue.divideAndRemainder(TEN);
                int digit = res[1].intValue() * 1000;
                digits.add(new Integer(digit));
                unscaledValue = res[0];
            } else if (nBaseScaleRemainder == 2) {
                BigInteger res[] = unscaledValue.divideAndRemainder(HUNDRED);
                int digit = res[1].intValue() * 100;
                digits.add(new Integer(digit));
                unscaledValue = res[0];
            } else if (nBaseScaleRemainder == 3) {
                BigInteger res[] = unscaledValue.divideAndRemainder(THOUSAND);
                int digit = res[1].intValue() * 10;
                digits.add(new Integer(digit));
                unscaledValue = res[0];
            }
            while (!unscaledValue.equals(BigInteger.ZERO)) {
                BigInteger res[] = unscaledValue.divideAndRemainder(TEN_THOUSAND);
                digits.add(new Integer(res[1].intValue()));
                unscaledValue = res[0];
            }
            dos.writeInt(8 + (2 * digits.size()));
            dos.writeShort(digits.size());
            dos.writeShort(digits.size() - nBaseScale - 1);
            dos.writeShort(signum == 1 ? 0x0000 : 0x4000);
            dos.writeShort(scale);
            //StringBuffer log = new StringBuffer("Writing BigDecimal ")
            //    .append(o.toString())
            //    .append(" as (digitCount = ")
            //    .append(Integer.toString(digits.size()))
            //    .append(", weight = ")
            //    .append(Integer.toString(digits.size() - nBaseScale - 1))
            //    .append(", sign = ")
            //    .append(Integer.toString(signum == 1 ? 0x0000 : 0x4000))
            //    .append(", dscale = ")
            //    .append(Integer.toString(scale))
            //    .append(")");
            for (int i = digits.size() - 1; i >= 0; i--) {
                int digit = ((Integer) digits.get(i)).intValue();
                dos.writeShort(digit);
            //    log.append(" " + digit);
            }
            //LOG.error(log.toString());
        } else {
            throw new IllegalArgumentException("Cannot store values of type " + o.getClass());
        }
    }

    /**
     * {@inheritDoc}
     */
    protected int doIndirectionInserts(String name,
            IndirectionTableBatch table, List batches) throws SQLException {
        if (!table.getRowsToInsert().isEmpty()) {
            try {
                CopyManager copyManager = null;
                if (con instanceof PGConnection) {
                    copyManager = ((PGConnection) con).getCopyAPI();
                }
                if (copyManager == null) {
                    LOG.warn("Database is incompatible with the PostgreSQL COPY command - falling"
                            + " back to prepared statements");
                    super.doIndirectionInserts(name, table, batches);
                } else {
                    SensibleByteArrayOutputStream baos = new SensibleByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeBytes("PGCOPY\n");
                    dos.writeByte(255);
                    dos.writeBytes("\r\n");
                    dos.writeByte(0); // Signature done
                    dos.writeInt(0); // Flags - we aren't supplying OIDS
                    dos.writeInt(0); // Length of header extension
                    Iterator insertIter = table.getRowsToInsert().iterator();
                    while (insertIter.hasNext()) {
                        Row row = (Row) insertIter.next();
                        dos.writeShort(2);
                        dos.writeInt(4);
                        dos.writeInt(row.getLeft());
                        dos.writeInt(4);
                        dos.writeInt(row.getRight());
                    }
                    String sql = "COPY " + name + " (" + table.getLeftColName() + ", "
                        + table.getRightColName() + ") FROM STDIN BINARY";
                    dos.writeShort(-1);
                    dos.flush();
                    batches.add(new FlushJobPostgresCopyImpl(copyManager, sql,
                                baos.toByteArray()));
                }
            } catch (IOException e) {
                throw new SQLException(e.toString());
            }
        }
        return table.getRowsToInsert().size();
    }

    /**
     * {@inheritDoc}
     */
    protected int getTableSize(String name, Connection conn) throws SQLException {
        Statement s = conn.createStatement();
        ResultSet r = s.executeQuery("SELECT reltuples FROM pg_class WHERE relname = '"
                + name.toLowerCase() + "'");
        if (r.next()) {
            int returnValue = (int) r.getFloat(1);
            if (r.next()) {
                throw new SQLException("Too many results");
            }
            return returnValue;
        } else {
            throw new SQLException("No results");
        }
    }
}

