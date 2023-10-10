package it.pagopa.selfcare.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.jackson.DatabindCodec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class OnboardingFunctionConfig {

    @Produces
    public ObjectMapper objectMapper(){
        ObjectMapper mapper =  DatabindCodec.mapper();
        //mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES)); // mandatory config

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);// custom config
        mapper.registerModule(new JavaTimeModule());                               // custom config
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);            // custom config
       // mapper.registerModule(new Jdk8Module());                                   // custom config
        return mapper;
    }

}
