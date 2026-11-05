package com.se6362.kwic.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import java.lang.StringBuilder;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class DatabaseManager
{
    private static final RowMapper<WebPage> PAGE_MAPPER = (row, rowNo) -> new WebPage(row.getString("Title"),
            row.getString("Url"), row.getString("Description"));

    private final JdbcTemplate database;

    public DatabaseManager(JdbcTemplate database)
    {
        this.database = database;
    }

    @Transactional
    public void insertWebpages(PageList pages)
    {
        String sql = "INSERT INTO WEB_DATA (title, url, description) VALUES (?, ?, ?);";
        database.batchUpdate(sql, new BatchPreparedStatementSetter()
        {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException
            {
                ps.setString(1, pages.getPage(i).getTitle());
                ps.setString(2, pages.getPage(i).getUrl());
                ps.setString(3, pages.getPage(i).getDescriptor().toString());
            }

            @Override
            public int getBatchSize()
            {
                return pages.getPageCount();
            }
        });
    }

    @Transactional
    public void insertIndexEntries(PageList pages)
    {
        String sql = "INSERT INTO KWIC_INDEX (page_id, keyword, shift_amount) VALUES (?, ?, ?)";

        Map<String, Integer> pageIds = getPageIds();

        database.batchUpdate(sql, new BatchPreparedStatementSetter()
        {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException
            {
                ps.setInt(1, pageIds.get(pages.getPage(i).getUrl()).intValue());
                ps.setString(2, pages.getPage(i).getDescriptor().getFilteredWord(0));
                ps.setInt(3, ((ShiftedDescriptor) pages.getPage(i).getDescriptor()).getShiftAmount());
            }

            @Override
            public int getBatchSize()
            {
                return pages.getPageCount();
            }
        });
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getPageIds()
    {
        String sql = "SELECT id, url FROM WEB_DATA";

        return database.query(sql, rs ->
        {
            Map<String, Integer> pageIds = new HashMap<>();

            while (rs.next())
            {
                pageIds.put(rs.getString("url"), rs.getInt("id"));
            }

            return pageIds;
        });
    }

    @Transactional(readOnly = true)
    public PageList getSearchResults(String searchQuery)
    {
        PageList pageList = new PageList();
        QueryTree tree = new QueryTree(searchQuery);
        String sql = generateSearchSQL(tree);
        for (WebPage page : database.query(sql, PAGE_MAPPER))
        {
            pageList.appendPage(page);
        }
        return pageList;
    }

    private String generateSearchSQL(QueryTree tree)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT url, title, description ");
        sqlBuilder.append("FROM WEB_DATA ");
        sqlBuilder.append("WHERE id IN (").append(generateNodeSQL(tree.getRoot())).append(");");
        return sqlBuilder.toString();
    }

    private String generateNodeSQL(QueryTree.QueryNode node)
    {
        if (node instanceof QueryTree.KeywordNode keywordNode)
        {
            return generateKeywordNodeSQL(keywordNode);
        }
        else if (node instanceof QueryTree.BinaryNode binaryNode)
        {
            return generateBinaryNodeSQL(binaryNode);
        }
        else if (node instanceof QueryTree.NotNode notNode)
        {
            return generateNotNodeSQL(notNode);
        }
        else
        {
            return "";
        }
    }

    private String generateKeywordNodeSQL(QueryTree.KeywordNode node)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT page_id ");
        sqlBuilder.append("FROM KWIC_INDEX ");
        sqlBuilder.append("WHERE keyword = ").append(node.keyword()).append(';');
        return sqlBuilder.toString();
    }

    private String generateBinaryNodeSQL(QueryTree.BinaryNode node)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT page_id ");
        sqlBuilder.append("FROM KWIC_INDEX ");
        sqlBuilder.append("WHERE page_id IN (").append(generateNodeSQL(node.left())).append(") ");
        sqlBuilder.append(node.operator()).append(" page_id IN (").append(generateNodeSQL(node.right())).append(");");
        return sqlBuilder.toString();
    }

    private String generateNotNodeSQL(QueryTree.NotNode node)
    {
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT page_id ");
        sqlBuilder.append("FROM KWIC_INDEX ");
        sqlBuilder.append("WHERE page_id NOT IN (").append(generateNodeSQL(node.operand())).append(");");

        return sqlBuilder.toString();
    }
}
