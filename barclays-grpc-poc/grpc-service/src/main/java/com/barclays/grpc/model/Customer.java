
package com.barclays.grpc.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Map;

@Data
@Document(collection = "customers")
public class Customer {
    @Id
    private String id;
    private String customerId;
    private String name;
    private String segment;
    private Map<String, String> metadata;
}
