
package com.barclays.rest.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.Map;

@Data
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    private String transactionId;
    private String accountNumber;
    private Double amount;
    private String transactionType;
    private Date timestamp;
    private Map<String, String> metadata; // NOT INDEXED - causes performance issues
}
