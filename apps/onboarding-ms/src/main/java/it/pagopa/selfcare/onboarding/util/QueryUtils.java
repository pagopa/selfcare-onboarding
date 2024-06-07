package it.pagopa.selfcare.onboarding.util;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class QueryUtils {

    public static Document buildQuery(Map<String, String> parameters) {
        if (!parameters.isEmpty()) {
            return bsonToDocument(Filters.and(constructBsonFilter(parameters)));
        } else {
            return new Document();
        }
    }

    /**
     * The constructBsonFilter function takes a Map of parameters and returns a List of Bson objects.
     * The function iterates over the entries in the parameter map, and for each entry it creates
     * either an equality filter or a range filter depending on whether the key is &quot;from&quot; or &quot;to&quot;.
     */
    private static List<Bson> constructBsonFilter(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> {
                    if (StringUtils.equalsIgnoreCase(entry.getKey(), "from")) {
                        return Filters.gte(Onboarding.Fields.createdAt.name(), LocalDate.parse(entry.getValue(), DateTimeFormatter.ISO_LOCAL_DATE));
                    } else if (StringUtils.equalsIgnoreCase(entry.getKey(), "to")) {
                        return Filters.lt(Onboarding.Fields.createdAt.name(), LocalDate.parse(entry.getValue(), DateTimeFormatter.ISO_LOCAL_DATE).plusDays(1));
                    }
                    return Filters.eq(entry.getKey(), entry.getValue());
                }).toList();
    }

    private static Document bsonToDocument(Bson bson) {
        BsonDocument bsonDocument = bson.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
        DocumentCodec codec = new DocumentCodec();
        DecoderContext decoderContext = DecoderContext.builder().build();
        return codec.decode(new BsonDocumentReader(bsonDocument), decoderContext);
    }

    /**
     * The createMapForOnboardingQueryParameter function creates a map of query parameters for the Onboarding Collection.
     */
    public static Map<String, String> createMapForOnboardingQueryParameter(String productId, String taxCode, String status, String from, String to) {
        Map<String, String> queryParameterMap = new HashMap<>();
        Optional.ofNullable(productId).ifPresent(value -> queryParameterMap.put("productId", value));
        Optional.ofNullable(taxCode).ifPresent(value -> queryParameterMap.put("institution.taxCode", value));
        Optional.ofNullable(status).ifPresent(value -> queryParameterMap.put("status", value));
        Optional.ofNullable(from).ifPresent(value -> queryParameterMap.put("from", value));
        Optional.ofNullable(to).ifPresent(value -> queryParameterMap.put("to", value));
        return queryParameterMap;
    }

    public static Map<String, String> createMapForOnboardingsQueryParameter(String productId, String taxCode, String origin, String originId, String subunitCode) {
        Map<String, String> queryParameterMap = new HashMap<>();
        Optional.ofNullable(productId).ifPresent(value -> queryParameterMap.put("productId", value));
        Optional.ofNullable(taxCode).ifPresent(value -> queryParameterMap.put("institution.taxCode", value));
        Optional.ofNullable(origin).ifPresent(value -> queryParameterMap.put("institution.origin", value));
        Optional.ofNullable(originId).ifPresent(value -> queryParameterMap.put("institution.originId", value));
        Optional.ofNullable(subunitCode).ifPresent(value -> queryParameterMap.put("institution.subunitCode", value));
        return queryParameterMap;
    }

    public static Map<String, String> createMapForInstitutionOnboardingsQueryParameter(String taxCode, String subunitCode, String origin, String originId, OnboardingStatus status) {
        Map<String, String> queryParameterMap = new HashMap<>();
        Optional.ofNullable(taxCode).ifPresent(value -> queryParameterMap.put("institution.taxCode", value));
        Optional.ofNullable(subunitCode).ifPresent(value -> queryParameterMap.put("institution.subunitCode", value));
        Optional.ofNullable(origin).ifPresent(value -> queryParameterMap.put("institution.origin", value));
        Optional.ofNullable(originId).ifPresent(value -> queryParameterMap.put("institution.originId", value));
        Optional.ofNullable(status).ifPresent(value -> queryParameterMap.put("status", value.name()));
        return queryParameterMap;
    }

    public static Map<String, Object> createMapForOnboardingReject(String reasonForReject, String onboardingStatus) {
        Map<String, Object> queryParameterMap = new HashMap<>();
        Optional.ofNullable(reasonForReject).ifPresent(value -> queryParameterMap.put("reasonForReject", value));
        Optional.ofNullable(onboardingStatus).ifPresent(value -> queryParameterMap.put("status", value));
        queryParameterMap.put("updatedAt", LocalDateTime.now());
        return queryParameterMap;
    }

    public static Document buildSortDocument(String field, SortEnum order) {
        if(SortEnum.ASC == order) {
            return bsonToDocument(Sorts.ascending(field));
        }else{
            return bsonToDocument(Sorts.descending(field));
        }
    }

    public static Document buildUpdateDocument(Map<String, Object> parameters) {
        if (!parameters.isEmpty()) {
            return bsonToDocument(Updates.combine(constructBsonUpdate(parameters)));
        } else {
            return new Document();
        }
    }

    private static List<Bson> constructBsonUpdate(Map<String, Object> parameters) {
        return parameters.entrySet()
                .stream()
                .map(stringStringEntry -> Updates.set(stringStringEntry.getKey(), stringStringEntry.getValue()))
                .toList();
    }

}
