db = db.getSiblingDB("selcOnboarding");

db.onboardings.insertOne({
  createdAt: new Date(),
});

db.tokens.insertOne({
  createdAt: new Date(),
});

db = db.getSiblingDB("selcMsCore");


const institutionObject = {
  "_id": "0c7bcbdb-40f8-41d8-98e7-5952177820c0",
  "externalId": "13234472819",
  "origin": "SELC",
  "originId": "13234472819",
  "description": "test",
  "institutionType": "GPU",
  "digitalAddress": "1@1.com",
  "address": "via Roma 3",
  "zipCode": "23123",
  "taxCode": "13234472819",
  "city": "Milano",
  "county": "MI",
  "country": "IT",
  "geographicTaxonomies": [
    {
      "code": "ITA",
      "desc": "ITALIA"
    }
  ],
  "rea": "REA-13",
  "imported": false,
  "createdAt": new Date("2024-12-19T17:06:09.236676631Z"),
  "delegation": false,
  "updatedAt": new Date("2024-12-19T17:06:09.604853761Z"),
  "onboarding": [
    {
      "productId": "prod-pagopa",
      "tokenId": "5f77dfa3-0b3e-4576-b1b5-4a2c214a7856",
      "status": "ACTIVE",
      "billing": {
        "vatNumber": "13234472819",
        "recipientCode": "123334",
        "publicServices": false
      },
      "createdAt": new Date("2024-12-19T17:06:09.578122497Z"),
    }
  ]
};

db.institutions.insertOne(institutionObject);
