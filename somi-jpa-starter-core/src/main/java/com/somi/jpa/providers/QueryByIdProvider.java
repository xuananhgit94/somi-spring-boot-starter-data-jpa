package com.somi.jpa.providers;

import com.somi.jpa.constants.AppConstants;
import com.somi.jpa.enums.ErrorMessage;
import com.somi.jpa.exceptions.NativeQueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryByIdProvider {
    private static final Logger log = LoggerFactory.getLogger(QueryByIdProvider.class);

    private final Map<String, String> queries;

    public QueryByIdProvider(List<String> locations) {
        this(locations, new PathMatchingResourcePatternResolver());
    }

    public QueryByIdProvider(List<String> locations, ResourcePatternResolver resolver) {
        DocumentBuilder builder = newDocumentBuilder();
        Map<String, String> loaded = new HashMap<>();
        for (String location : locations) {
            log.info("Loading queries from {}", location);
            try {
                for (Resource resource : resolver.getResources(location)) {
                    loadFile(resource, builder, loaded);
                }
            } catch (IOException e) {
                throw NativeQueryException.wrap(e, ErrorMessage.LOAD_QUERY_ERROR, location);
            }
        }
        this.queries = Map.copyOf(loaded);
        log.info("Loaded {} queries", queries.size());
    }

    public boolean contains(String id) {
        return queries.containsKey(id);
    }

    public String getQuery(String id) {
        String sql = queries.get(id);
        if (sql == null) {
            throw NativeQueryException.of(ErrorMessage.QUERY_ID_NOT_FOUND, id);
        }
        return sql;
    }

    private static void loadFile(Resource resource, DocumentBuilder builder, Map<String, String> queries) {
        String fileName = resource.getDescription();
        log.debug("Processing query file {}", fileName);
        Document document;
        try (InputStream in = resource.getInputStream()) {
            document = builder.parse(in);
        } catch (IOException | SAXException e) {
            throw NativeQueryException.wrap(e, ErrorMessage.FAIL_PARSE_QUERY, fileName);
        }
        NodeList queryNodes = document.getElementsByTagName(AppConstants.TAG_QUERY);
        for (int i = 0; i < queryNodes.getLength(); i++) {
            Element element = (Element) queryNodes.item(i);
            String id = element.getAttribute(AppConstants.ATTRIBUTE_QUERY).trim();
            String sql = element.getTextContent().trim();
            if (!StringUtils.hasText(id) || !StringUtils.hasText(sql)) {
                log.warn("Skipping <query> without id or SQL in file {}", fileName);
                continue;
            }
            if (queries.putIfAbsent(id, sql) != null) {
                throw NativeQueryException.of(ErrorMessage.DUPLICATE_QUERY_ID, id, fileName);
            }
            log.debug("Loaded query [{}] from {}", id, fileName);
        }
    }

    private static DocumentBuilder newDocumentBuilder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultNSInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Cannot create XML parser", e);
        }
    }
}
