
package com.uhf.dao;

import java.sql.Connection;
import java.util.List;

import com.uhf.utils.TagReply;

public interface TagDao {

    String getNextReferenceNo(
        String table,
        String column,
        String dateFormat,
        String prefix,
        String businessUnitRefCode,
        Connection connection
    );

    List<TagReply> findAll();

    WriteResult write(TagReply tag, String newHex);

    boolean verify(String expectedHex);

    String getLastReply();
}
