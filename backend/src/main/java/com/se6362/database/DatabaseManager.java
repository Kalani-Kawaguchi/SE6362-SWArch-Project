package com.se6362.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.se6362.search.PageRankStrategy;
import com.se6362.search.QueryNode;
import com.se6362.search.SearchSqlGenerator;
import com.se6362.web.*;

@Repository
public class DatabaseManager
{
    private static final RowMapper<WebPage> PAGE_MAPPER = (row, rowNo) -> new WebPage(row.getString("Title"),
            row.getString("Url"), row.getString("Description"), row.getInt("sponsorship_level"));

    private final JdbcTemplate database;

    public DatabaseManager(JdbcTemplate database)
    {
        this.database = database;
    }

    @Transactional
    public void addWebpages(PageList webpages, PageList indexEntries)
    {
        this.insertWebpages(webpages);
        this.insertIndexEntries(indexEntries);
    }

    @Transactional
    public void deleteWebpages(List<String> urls)
    {
        String sql = "DELETE FROM WEB_DATA WHERE url = ?";

        database.batchUpdate(sql, new BatchPreparedStatementSetter()
        {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException
            {
                ps.setString(1, urls.get(i));
            }

            @Override
            public int getBatchSize()
            {
                return urls.size();
            }
        });
    }

    @Transactional
    public void addSponsorship(WebPage page, int level)
    {
        String sql = "UPDATE WEB_DATA SET sponsorship_level = ? WHERE url = ?";
        database.update(sql, level, page.getUrl());
    }

    @Transactional
    public void incrementAccessFrequency(WebPage page)
    {
        String sql = "UPDATE WEB_DATA SET access_frequency = access_frequency + 1 WHERE url = ?";
        database.update(sql, page.getUrl());
    }

    @Transactional(readOnly = true)
    public PageList getSearchResults(QueryNode rootNode, PageRankStrategy sortStrategy, boolean isCaseSensitive)
    {
        PageList pageList = new PageList();

        SearchSqlGenerator sqlGenerator = new SearchSqlGenerator(isCaseSensitive);
        String sql = sqlGenerator.getSearchSQL(rootNode, sortStrategy);

        for (WebPage page : database.query(
                sql,
                PAGE_MAPPER,
                sqlGenerator.getKeywords().toArray()))
        {
            pageList.append(page);
        }

        return pageList;
    }

    public List<String> getAllUrls()
    {
        return database.query(
                "SELECT url FROM WEB_DATA ORDER BY id",
                (row, number) -> row.getString("url"));
    }

    private Map<String, Integer> getPageIds()
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

    private void insertWebpages(PageList pages)
    {
        String sql = "INSERT IGNORE INTO WEB_DATA (title, url, description) VALUES (?, ?, ?);";
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

    private void insertIndexEntries(PageList pages)
    {
        String sql = "INSERT IGNORE INTO KWIC_INDEX (page_id, keyword, shift_amount) VALUES (?, ?, ?)";
        System.out.println("Retrieving page IDs...");
        Map<String, Integer> pageIds = getPageIds();
        System.out.println("Page ids have been retrieved.");

        database.batchUpdate(sql, new BatchPreparedStatementSetter()
        {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException
            {
                ps.setInt(1, pageIds.get(pages.getPage(i).getUrl()).intValue());
                ps.setString(2, pages.getPage(i).getDescriptor().getWord(0));
                ps.setInt(3, ((ShiftedDescriptor) pages.getPage(i).getDescriptor()).getShiftAmount());
            }

            @Override
            public int getBatchSize()
            {
                return pages.getPageCount();
            }
        });
    }

}
