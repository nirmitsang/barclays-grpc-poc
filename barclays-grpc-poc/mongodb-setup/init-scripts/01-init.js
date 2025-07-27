db = db.getSiblingDB('corporate_banking');

// Create indexes (metadata fields intentionally NOT indexed)
db.customers.createIndex({ "customerId": 1 });
db.transactions.createIndex({ "accountNumber": 1 });
db.transactions.createIndex({ "timestamp": -1 });

// Insert test customers
for (let i = 1; i <= 1000; i++) {
    db.customers.insertOne({
        customerId: "CUST" + i.toString().padStart(6, '0'),
        name: "Corporate Customer " + i,
        segment: ["Corporate", "SME", "International"][i % 3],
        metadata: {
            industry: ["Banking", "Technology", "Retail"][i % 3],
            riskRating: ["Low", "Medium", "High"][i % 3],
            region: ["UK", "EU", "US", "APAC"][i % 4]
        }
    });
}

// Insert test transactions (with metadata that causes performance issues)
for (let i = 1; i <= 10000; i++) {
    db.transactions.insertOne({
        transactionId: "TXN" + i.toString().padStart(8, '0'),
        accountNumber: "ACC" + (Math.floor(Math.random() * 1000) + 1).toString().padStart(6, '0'),
        amount: Math.floor(Math.random() * 100000) + 100,
        transactionType: ["PAYMENT", "TRANSFER", "DEPOSIT"][i % 3],
        timestamp: new Date(),
        metadata: {
            channel: ["Online", "Branch", "Mobile"][i % 3],
            approvalCode: "APPR" + Math.random().toString(36).substring(7),
            category: ["Operational", "Investment", "Regulatory"][i % 3]
        }
    });
}

print("Test data loaded: 1000 customers, 10000 transactions");