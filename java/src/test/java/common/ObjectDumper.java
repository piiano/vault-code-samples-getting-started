package common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectDumper {

    void dump(String name, Object obj) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        var json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);

        System.out.printf("%s:\n%s\n", name, json);
    }
}
