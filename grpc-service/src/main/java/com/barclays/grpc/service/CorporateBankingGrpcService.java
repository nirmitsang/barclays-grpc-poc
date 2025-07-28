
package com.barclays.grpc.service;

import com.barclays.grpc.generated.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import java.util.*;

@GrpcService
@RequiredArgsConstructor
public class CorporateBankingGrpcService extends CorporateBankingServiceGrpc.CorporateBankingServiceImplBase {
    
    private final MongoTemplate mongoTemplate;
    
    @Override
    public void getMultipleCustomers(MultiCustomerRequest request, StreamObserver<MultiCustomerResponse> responseObserver) {
        long startTime = System.currentTimeMillis();
        
        // gRPC Optimization: Use single query with $in operator
        Query query = new Query(Criteria.where("customerId").in(request.getCustomerIdsList()));
        List<com.barclays.grpc.model.Customer> customers = mongoTemplate.find(query, com.barclays.grpc.model.Customer.class);
        
        // Filter by metadata in memory (more efficient than multiple DB queries)
        List<Customer> filteredCustomers = new ArrayList<>();
        for (com.barclays.grpc.model.Customer customer : customers) {
            boolean matches = true;
            for (Map.Entry<String, String> filter : request.getMetadataFiltersMap().entrySet()) {
                if (!filter.getValue().equals(customer.getMetadata().get(filter.getKey()))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                filteredCustomers.add(convertToProtoCustomer(customer));
            }
        }
        
        long queryTime = System.currentTimeMillis() - startTime;
        
        MultiCustomerResponse response = MultiCustomerResponse.newBuilder()
                .addAllCustomers(filteredCustomers)
                .setTotalCount(filteredCustomers.size())
                .setQueryTimeMs(queryTime)
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    @Override
    public void searchTransactions(TransactionSearchRequest request, StreamObserver<TransactionSearchResponse> responseObserver) {
        long startTime = System.currentTimeMillis();
        
        // Build optimized query
        Query query = new Query();
        if (!request.getAccountNumbersList().isEmpty()) {
            query.addCriteria(Criteria.where("accountNumber").in(request.getAccountNumbersList()));
        }
        
        // Fetch all matching transactions
        List<com.barclays.grpc.model.Transaction> transactions = mongoTemplate.find(query, com.barclays.grpc.model.Transaction.class);
        
        // Filter by metadata in memory
        List<Transaction> filteredTransactions = new ArrayList<>();
        for (com.barclays.grpc.model.Transaction transaction : transactions) {
            boolean matches = true;
            for (Map.Entry<String, String> filter : request.getMetadataFiltersMap().entrySet()) {
                String metadataValue = transaction.getMetadata().get(filter.getKey());
                if (metadataValue == null || !metadataValue.contains(filter.getValue())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                filteredTransactions.add(convertToProtoTransaction(transaction));
            }
        }
        
        long queryTime = System.currentTimeMillis() - startTime;
        
        TransactionSearchResponse response = TransactionSearchResponse.newBuilder()
                .addAllTransactions(filteredTransactions)
                .setTotalCount(filteredTransactions.size())
                .setQueryTimeMs(queryTime)
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    
    private Customer convertToProtoCustomer(com.barclays.grpc.model.Customer customer) {
        return Customer.newBuilder()
                .setId(customer.getId())
                .setCustomerId(customer.getCustomerId())
                .setName(customer.getName())
                .setSegment(customer.getSegment())
                .putAllMetadata(customer.getMetadata())
                .build();
    }
    
    private Transaction convertToProtoTransaction(com.barclays.grpc.model.Transaction transaction) {
        return Transaction.newBuilder()
                .setId(transaction.getId())
                .setTransactionId(transaction.getTransactionId())
                .setAccountNumber(transaction.getAccountNumber())
                .setAmount(transaction.getAmount())
                .setTransactionType(transaction.getTransactionType())
                .setTimestamp(transaction.getTimestamp().toString())
                .putAllMetadata(transaction.getMetadata())
                .build();
    }
}
