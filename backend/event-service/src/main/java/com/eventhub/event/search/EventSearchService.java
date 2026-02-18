package com.eventhub.event.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Elasticsearch-backed full-text search for events.
 * Indexes event documents and provides multi-field fuzzy search
 * across title, description, tags, and category.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventSearchService {

    private final ElasticsearchClient esClient;

    private static final String INDEX_NAME = "events";

    @PostConstruct
    public void ensureIndex() {
        try {
            boolean exists = esClient.indices().exists(
                    ExistsRequest.of(e -> e.index(INDEX_NAME))
            ).value();

            if (!exists) {
                esClient.indices().create(CreateIndexRequest.of(c -> c
                        .index(INDEX_NAME)
                        .mappings(m -> m
                                .properties("title", p -> p.text(t -> t.analyzer("standard").boost(2.0)))
                                .properties("description", p -> p.text(t -> t.analyzer("standard")))
                                .properties("category", p -> p.keyword(k -> k))
                                .properties("tags", p -> p.keyword(k -> k))
                                .properties("status", p -> p.keyword(k -> k))
                                .properties("organizerId", p -> p.keyword(k -> k))
                                .properties("startDate", p -> p.date(d -> d))
                                .properties("availableSeats", p -> p.integer(i -> i))
                        )
                ));
                log.info("Elasticsearch index '{}' created", INDEX_NAME);
            }
        } catch (IOException e) {
            log.warn("Failed to initialize Elasticsearch index: {}. Full-text search will fall back to DB.", e.getMessage());
        }
    }

    /**
     * Index or update an event document in Elasticsearch.
     */
    public void indexEvent(EventDocument doc) {
        try {
            esClient.index(IndexRequest.of(i -> i
                    .index(INDEX_NAME)
                    .id(doc.id())
                    .document(doc)
            ));
            log.debug("Indexed event: {}", doc.id());
        } catch (IOException e) {
            log.warn("Failed to index event {}: {}", doc.id(), e.getMessage());
        }
    }

    /**
     * Remove an event from the search index.
     */
    public void removeEvent(String eventId) {
        try {
            esClient.delete(d -> d.index(INDEX_NAME).id(eventId));
            log.debug("Removed event from index: {}", eventId);
        } catch (IOException e) {
            log.warn("Failed to remove event {}: {}", eventId, e.getMessage());
        }
    }

    /**
     * Full-text search across title, description, tags.
     * Returns a list of matching event IDs.
     */
    public List<String> search(String query, int page, int size) {
        try {
            SearchResponse<EventDocument> response = esClient.search(SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .from(page * size)
                    .size(size)
                    .query(Query.of(q -> q
                            .bool(b -> b
                                    .must(m -> m
                                            .multiMatch(MultiMatchQuery.of(mm -> mm
                                                    .query(query)
                                                    .fields("title^3", "description", "tags^2", "category")
                                                    .fuzziness("AUTO")
                                            ))
                                    )
                                    .filter(f -> f
                                            .term(t -> t.field("status").value("PUBLISHED"))
                                    )
                            )
                    ))
            ), EventDocument.class);

            return response.hits().hits().stream()
                    .map(Hit::id)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.warn("Elasticsearch search failed, returning empty: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public record EventDocument(
            String id,
            String title,
            String description,
            String category,
            List<String> tags,
            String status,
            String organizerId,
            String startDate,
            int availableSeats
    ) {}
}
