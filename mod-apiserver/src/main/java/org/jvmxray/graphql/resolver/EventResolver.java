package org.jvmxray.graphql.resolver;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import graphql.schema.DataFetchingEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class EventResolver {

    private final CqlSession session;

    public EventResolver(CqlSession session) {
        this.session = session;
    }

    public Event eventByEventid(DataFetchingEnvironment env) {
        String eventid = env.getArgument("eventid");
        ResultSet resultSet = session.execute("SELECT * FROM jvmxray.events WHERE eventid = ?", eventid);
        Row row = resultSet.one();
        return row == null ? null : mapEvent(row);
    }

    public List<Event> eventsByEventtp(DataFetchingEnvironment env) {
        String eventtp = env.getArgument("eventtp");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (eventtp == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments eventtp, fromIndex, and endIndex cannot be null. eventtp="+eventtp+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEvents("eventtp", eventtp, fromIndex, endIndex);
    }

    public List<Event> eventsByLoggernamespace(DataFetchingEnvironment env) {
        String loggernamespace = env.getArgument("loggernamespace");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (loggernamespace == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. loggernamespace="+loggernamespace+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEvents("loggernamespace", loggernamespace, fromIndex, endIndex);
    }

    public List<Event> eventsByCategory(DataFetchingEnvironment env) {
        String cat = env.getArgument("cat");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (cat == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. cat="+cat+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEvents("cat", cat, fromIndex, endIndex);
    }

    public List<Event> eventsByP1(DataFetchingEnvironment env) {
        String p1 = env.getArgument("p1");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (p1 == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. p1="+p1+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEvents("p1", p1, fromIndex, endIndex);
    }

    public List<Event> eventsByP2(DataFetchingEnvironment env) {
        String p2 = env.getArgument("p2");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (p2 == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. p2="+p2+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEvents("p2", p2, fromIndex, endIndex);
    }

    public List<Event> eventsByP3(DataFetchingEnvironment env) {
        String p3 = env.getArgument("p3");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (p3 == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. p3="+p3+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEvents("p3", p3, fromIndex, endIndex);
    }

    public EventMeta eventMetaByEventidandLevel(DataFetchingEnvironment env) {
        String eventid = env.getArgument("eventid");
        int level = env.getArgument("level");
        ResultSet resultSet = session.execute("SELECT * FROM event_meta WHERE id = ? AND lvl = ?", eventid, level);
        Row row = resultSet.one();
        return row == null ? null : mapEventMeta(row);
    }

    public List<EventMeta> eventsMetaByEventid(DataFetchingEnvironment env) {
        String eventid = env.getArgument("eventid");
        ResultSet resultSet = session.execute("SELECT * FROM event_meta WHERE eventid = ?", eventid);
        return mapEventMetaList(resultSet);
    }

    public List<EventMeta> eventsMetaByClzldr(DataFetchingEnvironment env) {
        String clzldr = env.getArgument("clzldr");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (clzldr == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. clzldr="+clzldr+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEventMeta("clzldr", clzldr, fromIndex, endIndex);
    }

    public List<EventMeta> eventsMetaByClzcn(DataFetchingEnvironment env) {
        String clzcn = env.getArgument("clzcn");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (clzcn == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. clzcn="+clzcn+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEventMeta("clzcn", clzcn, fromIndex, endIndex);
    }

    public List<EventMeta> eventsMetaByClzmethnm(DataFetchingEnvironment env) {
        String clzmethnm = env.getArgument("clzmethnm");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (clzmethnm == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. clzmethnm="+clzmethnm+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEventMeta("clzmethnm", clzmethnm, fromIndex, endIndex);
    }

    public List<EventMeta> eventsMetaByClzmodnm(DataFetchingEnvironment env) {
        String clzmodnm = env.getArgument("clzmodnm");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (clzmodnm == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. clzmodnm="+clzmodnm+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEventMeta("clzmodnm", clzmodnm, fromIndex, endIndex);
    }

    public List<EventMeta> eventsMetaByClzfilenm(DataFetchingEnvironment env) {
        String clzfilenm = env.getArgument("clzfilenm");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (clzfilenm == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. clzfilenm="+clzfilenm+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEventMeta("clzfilenm", clzfilenm, fromIndex, endIndex);
    }

    public List<EventMeta> eventsMetaByClzlocation(DataFetchingEnvironment env) {
        String clzmodvr = env.getArgument("clzmodvr");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (clzmodvr == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. clzmodvr="+clzmodvr+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEventMeta("clzmodvr", clzmodvr, fromIndex, endIndex);
    }

    public List<EventMeta> eventsMetaByClzmodvr(DataFetchingEnvironment env) {
        String clzlocation = env.getArgument("clzlocation");
        Long fromIndex = (Long)env.getArgument("fromIndex");
        Long endIndex = (Long)env.getArgument("endIndex");
        if (clzlocation == null || fromIndex == null || endIndex == null) {
            throw new IllegalArgumentException("Arguments type, fromIndex, and endIndex cannot be null. clzlocation="+clzlocation+" fromIndex="+fromIndex+" endIndex="+endIndex);
        }
        return queryEventMeta("clzlocation", clzlocation, fromIndex, endIndex);
    }

    private List<Event> queryEvents(String field, String value, Long fromIndex, Long endIndex) {
        int limit = (int)(endIndex - fromIndex);
        String query = String.format("SELECT * FROM jvmxray.events WHERE %s = ? LIMIT ? ALLOW FILTERING", field);
        ResultSet resultSet = session.execute(query, value, limit);
        return StreamSupport.stream(resultSet.spliterator(), false)
                .map(this::mapEvent)
                .collect(Collectors.toList());
    }


    private List<EventMeta> queryEventMeta(String field, String value, Long fromIndex, Long endIndex) {
        long limit = endIndex - fromIndex;
        ResultSet resultSet = session.execute("SELECT * FROM jvmxray.event_meta WHERE ? = ? LIMIT ? ALLOW FILTERING", field, value, limit);
        return StreamSupport.stream(resultSet.spliterator(), false)
                .map(this::mapEventMeta)
                .collect(Collectors.toList());
    }

    private Event mapEvent(Row row) {
        return new Event(
                row.getString("eventid"),
                row.getLong("ts"),
                row.getString("eventtp"),
                row.getString("loglevel"),
                row.getString("loggernamespace"),
                row.getString("aid"),
                row.getString("cat"),
                row.getString("p1"),
                row.getString("p2"),
                row.getString("p3")
        );
    }

    private EventMeta mapEventMeta(Row row) {
        return new EventMeta(
                row.getString("eventid"),
                row.getInt("level"),
                row.getString("clzldr"),
                row.getString("clzcn"),
                row.getString("clzmethnm"),
                row.getString("clzmodnm"),
                row.getString("clzmodvr"),
                row.getString("clzfilenm"),
                row.getInt("clzlineno"),
                row.getString("clzlocation"),
                row.getString("clznative")
        );
    }

    private List<Event> mapEventList(ResultSet resultSet) {
        List<Event> events = new ArrayList<>();
        for (Row row : resultSet) {
            events.add(mapEvent(row));
        }
        return events;
    }

    private List<EventMeta> mapEventMetaList(ResultSet resultSet) {
        List<EventMeta> eventMetas = new ArrayList<>();
        for (Row row : resultSet) {
            eventMetas.add(mapEventMeta(row));
        }
        return eventMetas;
    }
}
