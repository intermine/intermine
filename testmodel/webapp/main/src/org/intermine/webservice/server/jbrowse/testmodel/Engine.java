package org.intermine.webservice.server.jbrowse.testmodel;

import static java.lang.String.format;
import static org.intermine.pathquery.Constraints.eq;
import static org.intermine.webservice.server.jbrowse.Queries.pathQueryToOSQ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Book;
import org.intermine.model.testmodel.Chapter;
import org.intermine.model.testmodel.Composition;
import org.intermine.model.testmodel.FavouritePassage;
import org.intermine.model.testmodel.Line;
import org.intermine.model.testmodel.Poem;
import org.intermine.model.testmodel.Section;
import org.intermine.model.testmodel.Text;
import org.intermine.model.testmodel.TextLocation;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryCast;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryEvaluable;
import org.intermine.objectstore.query.QueryExpression;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryFunction;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintRange;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.CacheMap;
import org.intermine.webservice.server.jbrowse.Command;
import org.intermine.webservice.server.jbrowse.CommandRunner;
import org.intermine.webservice.server.jbrowse.Segment;


public class Engine extends CommandRunner {

    private static final Logger LOG = Logger.getLogger(Engine.class);
    private static final Map<Command, Map<String, Object>> STATS_CACHE =
            new CacheMap<Command, Map<String, Object>>("jbrowse.testmodel.engine.STATS_CACHE");

    public Engine(InterMineAPI api) {
        super(api);
    }

    // A Query that produces a single row: (featureDensity :: double, featureCount :: integer)
    @Override
    protected Query getStatsQuery(Command command) {

        Model model = getAPI().getModel();
        String featureType = command.getType("Section");
        ClassDescriptor sec = model.getClassDescriptorByName("Section");
        ClassDescriptor fcd = model.getClassDescriptorByName(featureType);
        // Check type conditions.
        if (fcd == null) {
            throw new RuntimeException(featureType + " is not in the model.");
        }
        if (fcd != sec && !fcd.getAllSuperDescriptors().contains(sec)) { 
            throw new RuntimeException(featureType + " is not a textual section.");
        }

        QueryClass books = new QueryClass(Book.class);
        QueryClass text = new QueryClass(Text.class);

        // A query to get the feature-count for the current domain.
        Query countQ = getFeatureQuery(command);
        QueryEvaluable count = new QueryFunction();
        countQ.clearSelect();
        countQ.addToSelect(count);

        // A query to get the size of the domain.
        Query subq_2 = new Query();
        QueryEvaluable length;
        Segment seg = command.getSegment();
        if (seg.getWidth() == null || seg == Segment.GLOBAL_SEGMENT) {

            subq_2.addFrom(books);
            subq_2.addFrom(text);
            

            length = new QueryFunction(new QueryField(text, "length"), QueryFunction.SUM);
            if (seg.getStart() != null) {
                subq_2.addToSelect(new QueryExpression(length, QueryExpression.SUBTRACT, new QueryValue(seg.getStart())));
            } else if (seg.getEnd() != null) {
                subq_2.addToSelect(new QueryExpression(new QueryValue(seg.getStart()), QueryExpression.SUBTRACT, length));
            } else {
                subq_2.addToSelect(length);
            }

            ConstraintSet cs = constrainToLanguage(books, text, command.getDomain());
            if (seg.getSection() != null) {
                cs.addConstraint(new SimpleConstraint(
                        new QueryField(books, "identifier"),
                        ConstraintOp.EQUALS,
                        new QueryValue(seg.getSection())));
            }

            subq_2.setConstraint(cs);
        } else {
            length = new QueryValue(seg.getWidth());
            subq_2.addToSelect(length);
        }

        // A query that returns one row, with the density and feature count.
        Query q = new Query();
        q.addFrom(countQ);
        q.addFrom(subq_2);
        q.addToSelect(new QueryExpression(
            new QueryCast(new QueryField(countQ, count), Double.class),
            QueryExpression.DIVIDE,
            new QueryCast(new QueryField(subq_2, length), Double.class)));
        q.addToSelect(new QueryField(countQ, count));

        return q;
    }

    private ConstraintSet constrainToLanguage(QueryClass books, QueryClass text, String domain) {
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(new ContainsConstraint(
            new QueryObjectReference(books, "text"),
            ConstraintOp.CONTAINS, text));
        cs.addConstraint(new SimpleConstraint(
            new QueryField(text, "language"),
            ConstraintOp.EQUALS,
            new QueryValue(domain)));
        return cs;
    }

    @Override
    public void reference(Command command) {
        Segment seg = command.getSegment();
        Integer start = (seg.getStart() == null) ? 0 : seg.getStart();
        Integer end = seg.getEnd();

        Query q = getReferenceQuery(command);

        List<Object> results = getAPI().getObjectStore().executeSingleton(q);
        Iterator<Object> it = results.iterator();
        while (it.hasNext()) {
            Book book = (Book) it.next();
            Map<String, Object> ref = new HashMap<String, Object>();
            int bookLength = book.getText().getLength();
            Integer bookEnd = (end == null) ? bookLength : Math.min(end, bookLength);
            ref.put("start", start);
            ref.put("end", bookEnd);
            ref.put("seq", String.valueOf(book.getText().getText().subSequence(start, bookEnd)));
            onData(ref, it.hasNext());
        }
    }

    private Query getReferenceQuery(Command command) {
        PathQuery pq = new PathQuery(getAPI().getModel());
        pq.addView("Book.id");
        pq.addConstraint(Constraints.eq("Book.identifier", command.getSegment().getSection()));
        pq.addConstraint(Constraints.eq("Book.text.language", command.getDomain()));
        LOG.info("REFERENCE QUERY: " + pq.toXml());
        return pathQueryToOSQ(pq);
    }

    @Override
    public void features(Command command) {
        if (command.getSegment() != Segment.NEGATIVE_SEGMENT) {
            Query q = getFeatureQuery(command);
            Iterator<Object> it = getResults(q).iterator();
            while (it.hasNext()) {
                Section s = (Section) it.next();
                onData(makeSectionFeature(s), it.hasNext());
            }
        }
    }

    private Map<String, Object> makeSectionFeature(Section s) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("type", s.getClass().getName());
        ret.put("name", s.getName());
        ret.put("uniqueID", s.getName());
        if (s instanceof Poem) {
            ret.put("name", ((Poem) s).getTitle());
        } else if (s instanceof Chapter) {
            ret.put("name", ((Chapter) s).getTitle());
        } else if (s instanceof Line) {
            ret.put("name", ((Line) s).getNumber());
        }
        if (s instanceof FavouritePassage) {
            ret.put("score", ((FavouritePassage) s).getRating());
        }
        TextLocation loc = s.getTextLocation();
        ret.put("start", loc.getStart() - 1);
        ret.put("end", loc.getEnd());
        ret.put("strand", 1); // There are no reverse text features.
        List<Map<String, Object>> subsections = new ArrayList<Map<String, Object>>();
        for (Composition comp: s.getSubSections()) {
            if (comp instanceof Section) {
                subsections.add(makeSectionFeature((Section) comp));
            }
        }
        ret.put("subfeatures", subsections);
        return ret;
    }
 
    @Override
    protected PathQuery getFeaturePathQuery(Command command, Segment segment) {
        PathQuery pq = new PathQuery(getAPI().getModel());
        String type = command.getType("Section");
        pq.addView(format("%s.id", type));
        pq.addConstraint(eq(format("%s.text.language", type), command.getDomain()));
        if (segment != Segment.GLOBAL_SEGMENT)
            pq.addConstraint(makeRangeConstraint(type, segment));
        LOG.info("FEATURE QUERY: " + pq.toXml());
        return pq;
    }

    private PathConstraint makeRangeConstraint(String type, Segment segment) {
        return new PathConstraintRange(format("%s.textLocation", type),
                ConstraintOp.OVERLAPS, Collections.singleton(segment.toRangeString()));
    }

    // Run a query and get the list of objects it returns.
    private List<Object> getResults(Query q) {
        return getAPI().getObjectStore().executeSingleton(q);
    }


}
