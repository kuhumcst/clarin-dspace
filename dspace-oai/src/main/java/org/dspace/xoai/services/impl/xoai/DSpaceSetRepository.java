/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import com.lyncode.xoai.dataprovider.core.ListSetsResult;
import com.lyncode.xoai.dataprovider.core.Set;
import com.lyncode.xoai.dataprovider.services.api.SetRepository;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.xoai.data.DSpaceSet;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * based on class by Lyncode Development Team <dspace@lyncode.com>
 * modified for LINDAT/CLARIN
 */
public class DSpaceSetRepository implements SetRepository
{
    private static final Logger log = LogManager.getLogger(DSpaceSetRepository.class);

    private final Context _context;

    public DSpaceSetRepository(Context context)
    {
        _context = context;
    }

    private int getCommunityCount()
    {
        String query = "SELECT COUNT(*) as count FROM community";
        try
        {
            TableRowIterator iterator = DatabaseManager.query(_context, query);
            if (iterator.hasNext())
                return (int) iterator.next().getLongColumn("count");
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    private int getCollectionCount()
    {
        String query = "SELECT COUNT(*) as count FROM collection";
        try
        {
            TableRowIterator iterator = DatabaseManager.query(_context, query);
            if (iterator.hasNext())
                return (int) iterator.next().getLongColumn("count");
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Produce a list of DSpaceCommunitySet.  The list is a segment of the full
     * list of Community ordered by ID.
     *
     * @param offset start this far down the list of Community.
     * @param length return up to this many Sets.
     * @return some Sets representing the Community list segment.
     */
    private List<Set> community(int offset, int length)
    {
        List<Set> array = new ArrayList<Set>();
        StringBuffer query = new StringBuffer("SELECT community_id FROM community ORDER BY community_id");
        List<Serializable> params = new ArrayList<Serializable>();

        DatabaseManager.applyOffsetAndLimit(query,params,offset,length);

        try
        {
            TableRowIterator iterator = DatabaseManager.query(_context, query.toString(),
                    params.toArray());
            int i = 0;
            while (iterator.hasNext() && i < length)
            {
                TableRow row = iterator.next();
                int communityID = row.getIntColumn("community_id");
                Community community = Community.find(_context, communityID);
                array.add(DSpaceSet.newDSpaceCommunitySet(
                        community.getHandle(), community.getName()));
                i++;
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return array;
    }

    /**
     * Produce a list of DSpaceCollectionSet.  The list is a segment of the full
     * list of Collection ordered by ID.
     *
     * @param offset start this far down the list of Collection.
     * @param length return up to this many Sets.
     * @return some Sets representing the Collection list segment.
     */
    private List<Set> collection(int offset, int length)
    {
        List<Set> array = new ArrayList<Set>();
        StringBuffer query = new StringBuffer("SELECT collection_id FROM collection ORDER BY collection_id");
        List<Serializable> params = new ArrayList<Serializable>();

        DatabaseManager.applyOffsetAndLimit(query,params,offset,length);

        try
        {
            TableRowIterator iterator = DatabaseManager.query(_context, query.toString(),
                    params.toArray());
            int i = 0;
            while (iterator.hasNext() && i < length)
            {
                TableRow row = iterator.next();
                int collectionID = row.getIntColumn("collection_id");
                Collection collection = Collection.find(_context, collectionID);
                array.add(DSpaceSet.newDSpaceCollectionSet(
                        collection.getHandle(),
                        collection.getName()));
                i++;
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return array;
    }

    @Override
    public ListSetsResult retrieveSets(int offset, int length)
    {
        // Only database sets (virtual sets are added by lyncode common library)
        log.debug("Querying sets. Offset: " + offset + " - Length: " + length);
        List<Set> array = new ArrayList<Set>();
        int communityCount = this.getCommunityCount();
        log.debug("Communities: " + communityCount);
        int collectionCount = this.getCollectionCount();
        log.debug("Collections: " + collectionCount);

        if (offset < communityCount)
        {
            if (offset + length > communityCount)
            {
                // Add some collections
                List<Set> tmp = community(offset, length);
                array.addAll(tmp);
                array.addAll(collection(0, length - tmp.size()));
            }
            else
                array.addAll(community(offset, length));
        }
        else if (offset < communityCount + collectionCount)
        {
            array.addAll(collection(offset - communityCount, length));
        }
        log.debug("Has More Results: "
                + ((offset + length < communityCount + collectionCount) ? "Yes"
                        : "No"));
        return new ListSetsResult(offset + length < communityCount
                + collectionCount, array, communityCount + collectionCount);
    }

    @Override
    public boolean supportSets()
    {
        return true;
    }

    @Override
    public boolean exists(String setSpec)
    {
		try {
			DSpaceObject dso = HandleManager.resolveToObject(_context, setSpec
					.replace("hdl_", "").replace("_", "/"));
			if (dso == null
					|| !(dso instanceof Collection || dso instanceof Community)) {
				return false;
			}
			return true;
		} catch (Exception ex)
		{
			log.error(ex.getMessage(), ex);
		}
		return false;
	}

}
