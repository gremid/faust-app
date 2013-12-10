package de.faustedition.transcript;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Range;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import de.faustedition.document.Documents;
import de.faustedition.index.Index;
import de.faustedition.text.NamespaceMapping;
import de.faustedition.text.TextAnnotationEnd;
import de.faustedition.text.TextAnnotationStart;
import de.faustedition.text.TextContent;
import de.faustedition.text.TextToken;
import de.faustedition.text.TextTokenPredicates;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.util.NumericUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.faustedition.text.NamespaceMapping.TEI_NS_URI;
import static de.faustedition.text.NamespaceMapping.map;


/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Singleton
public class VerseIndex extends AbstractIdleService {

    private static final Logger LOG = Logger.getLogger(VerseIndex.class.getName());

    private static final Pattern VERSE_NUMBER_PATTERN = Pattern.compile("[0-9]+");

    private final Index index;
    private final Transcripts transcripts;

    private final Predicate<TextToken> versePredicate;
    private final String verseNumberAttribute;

    @Inject
    public VerseIndex(Index index, Transcripts transcripts, NamespaceMapping namespaceMapping, EventBus eventBus) {
        this.index = index;
        this.transcripts = transcripts;

        this.versePredicate = TextTokenPredicates.xmlName(namespaceMapping, new QName(TEI_NS_URI, "l"));
        this.verseNumberAttribute = map(namespaceMapping, new QName(TEI_NS_URI, "n"));

        eventBus.register(this);
    }

    public Map<Long, Range<Integer>> query(final int verse, final int limit) throws Exception {
        return index.transaction(new Index.TransactionCallback<Map<Long, Range<Integer>>>() {
            @Override
            public Map<Long, Range<Integer>> doInTransaction() throws Exception {
                final Map<Long, Range<Integer>> result = Maps.newHashMap();

                final IndexSearcher searcher = searcher();
                final TopDocs topDocs = searcher.search(new TermQuery(new Term("n", NumericUtils.intToPrefixCoded(verse))), limit);
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    final Document indexDocument = searcher.doc(scoreDoc.doc);
                    result.put(Long.parseLong(indexDocument.get("id")), Range.closedOpen(
                            Integer.parseInt(indexDocument.get("start")),
                            Integer.parseInt(indexDocument.get("end"))
                    ));
                }

                return result;
            }
        });
    }

    @Subscribe
    @AllowConcurrentEvents
    public void documentsUpdated(final Documents.Updated updated) {
        try {
            index.transaction(new Index.TransactionCallback<Object>() {
                @Override
                public Object doInTransaction() throws Exception {
                    final Collection<Long> ids = updated.getIds();

                    delete(writer(), ids);

                    for (long documentId : ids) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("< #" + documentId);
                        }

                        final Map<String, String> verseNumbers = Maps.newHashMap();
                        final Map<String, Integer> verseOffsets = Maps.newHashMap();
                        int offset = 0;
                        for (TextToken token : transcripts.textual(documentId)) {
                            if (token instanceof TextContent) {
                                offset += ((TextContent) token).getContent().length();
                            } else if (versePredicate.apply(token)) {
                                final TextAnnotationStart annotationStart = (TextAnnotationStart) token;
                                final String id = annotationStart.getId();
                                final String verseNumber = annotationStart.getData().path(verseNumberAttribute).asText();

                                if (!verseNumber.isEmpty()) {
                                    verseNumbers.put(id, verseNumber);
                                    verseOffsets.put(id, offset);
                                }
                            } else if (token instanceof TextAnnotationEnd) {
                                final String id = ((TextAnnotationEnd) token).getId();
                                final String verseNumber = verseNumbers.remove(id);
                                if (verseNumber == null) {
                                    continue;
                                }
                                final String start = Integer.toString(verseOffsets.remove(id));
                                final String end = Integer.toString(offset);
                                final Matcher verseNumberMatcher = VERSE_NUMBER_PATTERN.matcher(verseNumber);
                                while (verseNumberMatcher.find()) {
                                    try {
                                        final int n = Integer.parseInt(verseNumberMatcher.group());

                                        final Document indexDocument = new Document();
                                        indexDocument.add(new Field("type", "verse", Field.Store.YES, Field.Index.NOT_ANALYZED));
                                        indexDocument.add(new Field("id", Long.toString(documentId), Field.Store.YES, Field.Index.NOT_ANALYZED));
                                        indexDocument.add(new Field("start", start, Field.Store.YES, Field.Index.NO));
                                        indexDocument.add(new Field("end", end, Field.Store.YES, Field.Index.NO));
                                        indexDocument.add(new NumericField("n", Field.Store.YES, true).setIntValue(n));

                                        writer().addDocument(indexDocument);
                                    } catch (NumberFormatException e) {
                                    }
                                }
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            if (LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Error while updating document index", e);
            }
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void documentsRemoved(final Documents.Removed removed) {
        try {
            index.transaction(new Index.TransactionCallback<Object>() {
                @Override
                public Object doInTransaction() throws Exception {
                    delete(writer(), removed.getIds());
                    return null;
                }
            });
        } catch (Exception e) {
            if (LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "Error while updating document index", e);
            }
        }
    }

    protected void delete(IndexWriter writer, Collection<Long> ids) throws IOException {
        int ni = ids.size();
        final BooleanQuery[] queries = new BooleanQuery[ni];
        final BooleanClause typeClause = new BooleanClause(new TermQuery(new Term("type", "verse")), BooleanClause.Occur.MUST);
        for (long id : ids) {
            queries[--ni] = new BooleanQuery();
            queries[ni].add(new BooleanClause(new TermQuery(new Term("id", Long.toString(id))), BooleanClause.Occur.MUST));
            queries[ni].add(typeClause);
        }
        writer.deleteDocuments(queries);
    }

    @Override
    protected void startUp() throws Exception {
    }

    @Override
    protected void shutDown() throws Exception {
    }
}