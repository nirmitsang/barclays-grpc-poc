package com.barclays.grpc.client;

import com.barclays.grpc.generated.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GrpcPerformanceClient {
    
    private final ManagedChannel channel;
    private final CorporateBankingServiceGrpc.CorporateBankingServiceBlockingStub blockingStub;

    public GrpcPerformanceClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = CorporateBankingServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public MultiCustomerResponse testMultiCustomerQuery() {
        // Create list of 50 customer IDs
        String[] customerIds = new String[50];
        for (int i = 1; i <= 50; i++) {
            customerIds[i-1] = String.format("CUST%06d", i);
        }

        // Create metadata filters
        Map<String, String> metadataFilters = new HashMap<>();
        metadataFilters.put("industry", "Banking");
        metadataFilters.put("riskRating", "Low");

        MultiCustomerRequest request = MultiCustomerRequest.newBuilder()
                .addAllCustomerIds(Arrays.asList(customerIds))
                .putAllMetadataFilters(metadataFilters)
                .build();

        return blockingStub.getMultipleCustomers(request);
    }

    public TransactionSearchResponse testTransactionSearch() {
        Map<String, String> metadataFilters = new HashMap<>();
        metadataFilters.put("channel", "Online");
        metadataFilters.put("category", "Operational");
        metadataFilters.put("approvalCode", "APPR");

        TransactionSearchRequest request = TransactionSearchRequest.newBuilder()
                .addAllAccountNumbers(Arrays.asList("ACC000001", "ACC000002", "ACC000003", "ACC000004", "ACC000005"))
                .putAllMetadataFilters(metadataFilters)
                .build();

        return blockingStub.searchTransactions(request);
    }

    public static void main(String[] args) {
        GrpcPerformanceClient client = new GrpcPerformanceClient("localhost", 9090);
        
        try {
            System.out.println("🧪 gRPC Performance Test");
            System.out.println("========================");
            
            // Test multi-customer query
            System.out.println("\nTesting multi-customer query...");
            MultiCustomerResponse customerResponse = client.testMultiCustomerQuery();
            System.out.println("Query Time: " + customerResponse.getQueryTimeMs() + "ms");
            System.out.println("Results: " + customerResponse.getTotalCount() + " customers");
            
            // Test transaction search
            System.out.println("\nTesting transaction search...");
            TransactionSearchResponse transactionResponse = client.testTransactionSearch();
            System.out.println("Query Time: " + transactionResponse.getQueryTimeMs() + "ms");
            System.out.println("Results: " + transactionResponse.getTotalCount() + " transactions");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                client.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}