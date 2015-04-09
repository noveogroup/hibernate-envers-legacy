package com.noveogroup.envers.initialize;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.envers.event.spi.EnversPostCollectionRecreateEventListenerImpl;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.persister.collection.CollectionPersister;

import java.util.Map;

/**
 * @author Andrey Sokolov
 */
public class CollectionEventInvoker {

    private final EnversPostCollectionRecreateEventListenerImpl collectionListener;
    private final EventSource source;

    public CollectionEventInvoker(final EnversPostCollectionRecreateEventListenerImpl collectionListener,
                                  final EventSource source) {
        this.collectionListener = collectionListener;
        this.source = source;
    }

    public void invokeCollectionListener(final PersistentCollection collection) {

        final Map<PersistentCollection, CollectionEntry> collectionEntries =
                (Map<PersistentCollection, CollectionEntry>) source.getPersistenceContext().getCollectionEntries();

        final CollectionEntry collectionEntry = collectionEntries.get(collection);
        final CollectionPersister collectionPersister = collectionEntry.getCurrentPersister();

        final PostCollectionRecreateEvent event = new PostCollectionRecreateEvent(collectionPersister,
                collection, source);
        collectionListener.onPostRecreateCollection(event);
    }
}
