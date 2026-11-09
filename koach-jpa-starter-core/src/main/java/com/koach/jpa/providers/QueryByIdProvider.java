package com.koach.jpa.providers;
import com.koach.jpa.configs.NativeQueryException;
import com.koach.jpa.constants.AppConstants;
import com.koach.jpa.enums.ErrorMessage;
import com.koach.jpa.exceptions.DefaultNativeQueryException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
@RequiredArgsConstructor
@Slf4j
public class QueryByIdProvider {
    private Map<String, String> queryMap;
    private final String[] classpathPaths;
    @PostConstruct
    public void loadQueries() {
        Map<String, String> tmpQueryMap = new HashMap<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            DocumentBuilder builder = DocumentBuilderFactory
                    .newDefaultNSInstance()
                    .newDocumentBuilder();
            for (String classpath : classpathPaths) {
                log.info("Loading queries from classpath: {}", classpath);
                Resource[] resources = resolver.getResources(classpath);
                for (Resource resource : resources) {
                    processQueryFile(resource, builder, tmpQueryMap);
                }
            }
            queryMap = Map.copyOf(tmpQueryMap);
            log.info("Loaded total {} queries", queryMap.size());
        } catch (Exception e) {
            if (e instanceof IOException) {
                log.warn("Failed to load queries from classpath: {}", e.getMessage());
            }
            DefaultNativeQueryException.fail(ErrorMessage.LOAD_QUERY_ERROR, e);
        }
    }
    private void processQueryFile(Resource resource, DocumentBuilder builder, Map<String, String> tmpQueryMap) {
        try (InputStream in = resource.getInputStream()) {
            log.debug("Processing query file: {}", resource.getFilename());
            Document doc = builder.parse(in);
            NodeList queryNodes = doc.getElementsByTagName(AppConstants.TAG_QUERY);
            for (int i = 0; i < queryNodes.getLength(); i++) {
                Element element = (Element) queryNodes.item(i);
                String id = element.getAttribute(AppConstants.ATTRIBUTE_QUERY).trim();
                String sql = element.getTextContent().trim();
                if (!StringUtils.hasText(id) || !StringUtils.hasText(sql)) {
                    log.warn("Skipping invalid <query> node: missing id or content in file {}", resource.getFilename());
                    continue;
                }
                if (tmpQueryMap.containsKey(id)) {
                    DefaultNativeQueryException.fail(ErrorMessage.DUPLICATE_QUERY_ID, id, resource.getFilename());
                }
                tmpQueryMap.put(id, sql);
                log.debug("Loaded query: [{}] from file: {}", id, resource.getFilename());
            }
        } catch (NativeQueryException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse query file: {}", resource.getFilename(), ex);
            DefaultNativeQueryException.fail(ErrorMessage.FAIL_PARSE_QUERY,resource.getFilename(), ex);
        }
    }
    public String getQuery(String id) {
        return Optional.ofNullable(queryMap.get(id)).orElseThrow(() -> DefaultNativeQueryException.of(ErrorMessage.QUERY_ID_NOT_FOUND, id));
    }
}
