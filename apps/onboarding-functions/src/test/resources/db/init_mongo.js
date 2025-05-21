db = db.getSiblingDB("selcOnboarding");

db.onboardings.insertOne({
  createdAt: new Date(),
});

db.tokens.insertOne({
  createdAt: new Date(),
});