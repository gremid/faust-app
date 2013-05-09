package de.faustedition.graph;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import de.faustedition.document.ArchiveCollection;
import de.faustedition.document.MaterialUnitCollection;
import de.faustedition.genesis.GeneticSourceCollection;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.neo4j.graphdb.Direction.OUTGOING;

@Component
public class Graph {
    private static final Logger LOG = Logger.getLogger(Graph.class.getName());

	public static final String PREFIX = "faust";

	private static final RelationshipType ROOT_RT = new FaustRelationshipType("root");

	private static final String ROOT_NAME_PROPERTY = ROOT_RT.name() + ".name";
	private static final String ARCHIVES_ROOT_NAME = PREFIX + ".archives";
	private static final String MATERIAL_UNITS_ROOT_NAME = PREFIX + ".material-units";
	private static final String GENETIC_SOURCES_ROOT_NAME = PREFIX + ".genetic-sources";

	private final GraphDatabaseService db;

    public static <T> T execute(GraphDatabaseService db, final Transaction<T> tx) {
        Stopwatch sw = null;
        org.neo4j.graphdb.Transaction transaction = null;
        try {
            try {
                if (LOG.isLoggable(Level.FINE)) {
                    sw = new Stopwatch();
                    sw.start();
                }

                transaction = db.beginTx();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Started transaction for {0}", tx);
                }

                final T result = tx.execute(new Graph(db));

                transaction.success();
                if (LOG.isLoggable(Level.FINE)) {
                    sw.stop();
                    LOG.log(Level.FINE, "Committed transaction for {0} after {1}", new Object[] { tx, sw });
                }

                return result;
            } catch (Exception e) {
                if (transaction != null && tx.rollsBackOn(e)) {
                    transaction.failure();
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Rolled back transaction for " + tx, e);
                    }
                }
                throw e;
            } finally {
                if (transaction != null) {
                    transaction.finish();
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Finished transaction for {0}", tx);
                    }
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }


    public static abstract class Transaction<T> {

        public abstract T execute(Graph graph) throws Exception;

        public boolean rollsBackOn(Exception e) {
            return true;
        }
    }

    public Graph(GraphDatabaseService db) {
        this.db = db;
    }

    public GraphDatabaseService db() {
        return db;
    }

    public ArchiveCollection getArchives() {
        return new ArchiveCollection(root(ARCHIVES_ROOT_NAME));
    }

    public MaterialUnitCollection getMaterialUnits() {
        return new MaterialUnitCollection(root(MATERIAL_UNITS_ROOT_NAME));
    }

    public GeneticSourceCollection getGeneticSources() {
        return new GeneticSourceCollection(root(GENETIC_SOURCES_ROOT_NAME));
    }

    protected Node root(String rootName) {
		final Node referenceNode = db.getReferenceNode();
		for (Relationship r : referenceNode.getRelationships(ROOT_RT, OUTGOING)) {
			if (rootName.equals(r.getProperty(ROOT_NAME_PROPERTY))) {
				return r.getEndNode();
			}
		}

		Relationship r = referenceNode.createRelationshipTo(referenceNode.getGraphDatabase().createNode(), ROOT_RT);
		r.setProperty(ROOT_NAME_PROPERTY, rootName);
		return r.getEndNode();
	}
}
