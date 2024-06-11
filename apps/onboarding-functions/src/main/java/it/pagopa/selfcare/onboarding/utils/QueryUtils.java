package it.pagopa.selfcare.onboarding.utils;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.dto.NotificationToSendFilters;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class QueryUtils {

    public static Document buildQuery(Map<String, List<String>> parameters) {
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
    private static List<Bson> constructBsonFilter(Map<String, List<String>> parameters) {
        return parameters.entrySet().stream()
                .map(entry -> {
                    List<String> values = entry.getValue();
                    if ( values.size() == 1 ) {
                        if (StringUtils.equalsIgnoreCase(entry.getKey(), "from")) {
                            return Filters.gte("createdAt", LocalDate.parse(entry.getValue().get(0), DateTimeFormatter.ISO_LOCAL_DATE));
                        } else if (StringUtils.equalsIgnoreCase(entry.getKey(), "to")) {
                            return Filters.lt("createdAt", LocalDate.parse(entry.getValue().get(0), DateTimeFormatter.ISO_LOCAL_DATE).plusDays(1));
                        }
                        return Filters.eq(entry.getKey(), entry.getValue().get(0));
                    } else {
                        return Filters.or(
                                values.stream()
                                        .map( value -> Filters.eq(entry.getKey(), value))
                                        .toList()
                        );
                    }
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
    public static Map<String, List<String>> createMapForOnboardingQueryParameter(NotificationToSendFilters filters) {
        Map<String, List<String>> queryParameterMap = new HashMap<>();


        Optional.ofNullable(filters.getProductId()).ifPresent(value -> queryParameterMap.put("productId", List.of(value)));
        Optional.ofNullable(filters.getInstitutionId()).ifPresent(value -> queryParameterMap.put("institution.id", List.of(value)));
        Optional.ofNullable(filters.getOnboardingId()).ifPresent(value -> queryParameterMap.put("id", List.of(value)));
        Optional.ofNullable(filters.getTaxCode()).ifPresent(value -> queryParameterMap.put("institution.taxCode", List.of(value)));
        Optional.ofNullable(filters.getStatus()).ifPresentOrElse(value -> queryParameterMap.put("status", List.of(value)),
                () -> queryParameterMap.put("status", List.of(OnboardingStatus.COMPLETED.name(), OnboardingStatus.DELETED.name())));
        Optional.ofNullable(filters.getFrom()).ifPresent(value -> queryParameterMap.put("from", List.of(value)));
        Optional.ofNullable(filters.getTo()).ifPresent(value -> queryParameterMap.put("to", List.of(value)));
        return queryParameterMap;
    }

    public static Document buildSortDocument(String field, SortEnum order) {
        if(SortEnum.ASC == order) {
            return bsonToDocument(Sorts.ascending(field));
        }else{
            return bsonToDocument(Sorts.descending(field));
        }
    }

}
