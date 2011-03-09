package org.modmine.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

/**
 * This is a generic pagination class.
 * @author Fengyuan Hu
 *
 */
public class PaginationUtil
{
    private int currentPageNum;
    private int displayRecordCount;
    private int pageCount;
    private int recordSize;
    private String method;

    private static final int DEFAULT_PAGESIZE = 10;

    /**
     * @param currentPageNum current page number
     * @param displayRecordCount the record count to display in each page/page size
     * @param pageCount total page count
     * @param recordSize total record size
     * @param method first/last page
     */
    public PaginationUtil(int currentPageNum, int displayRecordCount,
            int pageCount, int recordSize, String method) {
        super();
        this.currentPageNum = currentPageNum;
        this.displayRecordCount = displayRecordCount;
        this.pageCount = pageCount;
        this.recordSize = recordSize;
        this.method = method;
    }

    /**
     * @param currentPageNum current page number
     * @param displayRecordCount the record count to display in each page
     * @param method first/last page
     */
    public PaginationUtil(int currentPageNum, int displayRecordCount, String method) {
        super();
        this.currentPageNum = currentPageNum;
        this.displayRecordCount = displayRecordCount;
        this.method = method;
    }

    /**
     * @return the currentPageNum
     */
    public int getCurrentPageNum() {
        return currentPageNum;
    }

    /**
     * @param currentPageNum the currentPageNum to set
     */
    public void setCurrentPageNum(int currentPageNum) {
        this.currentPageNum = currentPageNum;
    }

    /**
     * @return the displayRecordCount
     */
    public int getDisplayRecordCount() {
        return displayRecordCount;
    }

    /**
     * @param displayRecordCount the displayRecordCount to set
     */
    public void setDisplayRecordCount(int displayRecordCount) {
        this.displayRecordCount = displayRecordCount;
    }

    /**
     * @return the pageCount
     */
    public int getPageCount() {
        return pageCount;
    }

    /**
     * @param pageCount the pageCount to set
     */
    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * @return the recordSize
     */
    public int getRecordSize() {
        return recordSize;
    }

    /**
     * @param recordSize the recordSize to set
     */
    public void setRecordSize(int recordSize) {
        this.recordSize = recordSize;
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * This is the core method to do pagination magic
     * @param dataList data in a list structure
     * @return List<Object> subDataList
     */
    public List<?> doPagination(List<?> dataList) {

        this.recordSize = dataList.size();

        if (dataList.size() == 0) {
            return null;
        }

        if (this.displayRecordCount < 0) {
            this.displayRecordCount = DEFAULT_PAGESIZE;
        }

        this.pageCount = this.recordSize / this.displayRecordCount;
        if (this.recordSize % this.displayRecordCount != 0) {
            this.pageCount++;
        }

        if (this.currentPageNum <= 0) {
            this.currentPageNum = 1;
        }

        // if page > total page, then page == last page
        if (this.currentPageNum > this.pageCount) {
            this.currentPageNum = this.pageCount;
        }

        if (method != null) {
            if ("first".equals(method)) {
                this.currentPageNum = 1;
            } else if ("last".equals(method)) {
                this.currentPageNum = this.pageCount;
            }
        }

        List<Object> subDataList = new ArrayList<Object>();
        for (int i = (this.currentPageNum - 1) * this.displayRecordCount; i < this.currentPageNum
                * this.displayRecordCount; i++) {
            if (i >= dataList.size()) {
                break;
            }
            subDataList.add(dataList.get(i));
        }
        return subDataList;
    }
}
