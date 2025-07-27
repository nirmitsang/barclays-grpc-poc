package com.barclays.rest.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;
import com.barclays.rest.model.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CorporateBankingController {
    
    private final MongoTemplate mongoTemplate;
    
    // This is THE problematic endpoint - multiple customers with metadata filters
    @PostMapping("/customers/multi-query")
    public MultiCustomerResponse getMultipleCustomers(@RequestBody MultiCustomerRequest request) {
        long startTime = System.currentTimeMillis();
        
        List<Customer> allCustomers = new ArrayList<>();
        
        // Problem: Multiple queries in a loop
        for (String customerId : request.getCustomerIds()) {
            Query query = new Query(Criteria.where("customerId").is(customerId));
            Customer customer = mongoTemplate.findOne(query, Customer.class);
            
            if (customer != null && request.getMetadataFilters() != null) {
                // Problem: Filtering on non-indexed metadata fields
                boolean matches = true;
                for (Map.Entry<String, String> filter : request.getMetadataFilters().entrySet()) {
                    if (!filter.getValue().equals(customer.getMetadata().get(filter.getKey()))) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    allCustomers.add(customer);
                }
            }
        }
        
        long queryTime = System.currentTimeMillis() - startTime;
        
        MultiCustomerResponse response = new MultiCustomerResponse();
        response.setCustomers(allCustomers);
        response.setTotalCount(allCustomers.size());
        response.setQueryTimeMs(queryTime);
        
        return response;
    }
    
    // Transaction search with metadata filters - another problematic query
    @PostMapping("/transactions/search")
    public TransactionSearchResponse searchTransactions(@RequestBody TransactionSearchRequest request) {
        long startTime = System.currentTimeMillis();
        
        Query query = new Query();
        
        // Add criteria
        if (request.getAccountNumbers() != null && !request.getAccountNumbers().isEmpty()) {
            query.addCriteria(Criteria.where("accountNumber").in(request.getAccountNumbers()));
        }
        
        // Problem: Querying non-indexed metadata fields
        if (request.getMetadataFilters() != null) {
            for (Map.Entry<String, String> filter : request.getMetadataFilters().entrySet()) {
                query.addCriteria(Criteria.where("metadata." + filter.getKey()).is(filter.getValue()));
            }
        }
        
        List<Transaction> transactions = mongoTemplate.find(query, Transaction.class);
        
        long queryTime = System.currentTimeMillis() - startTime;
        
        TransactionSearchResponse response = new TransactionSearchResponse();
        response.setTransactions(transactions);
        response.setTotalCount(transactions.size());
        response.setQueryTimeMs(queryTime);
        
        return response;
    }
    
    @Data
    public static class MultiCustomerRequest {
        private List<String> customerIds;
        private Map<String, String> metadataFilters;
    }
    
    @Data
    public static class MultiCustomerResponse {
        private List<Customer> customers;
        private int totalCount;
        private long queryTimeMs;
    }
    
    @Data
    public static class TransactionSearchRequest {
        private List<String> accountNumbers;
        private Map<String, String> metadataFilters;
    }
    
    @Data
    public static class TransactionSearchResponse {
        private List<Transaction> transactions;
        private int totalCount;
        private long queryTimeMs;
    }
}