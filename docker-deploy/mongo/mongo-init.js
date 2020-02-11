db.auth('root', 'example');

db = db.getSiblingDB('geo-mongo');

db.createRole({
  role : "readWriteSystem",
  privileges: [{
    resource: {
       db: "geo-mongo", collection: "system.indexes"
    },
    actions: [ "changeStream", "collStats", "convertToCapped", "createCollection", "createIndex", "dbHash", "dbStats", "dropCollection", "dropIndex", "emptycapped", "find", "insert", "killCursors", "listCollections", "listIndexes", "planCacheRead", "remove", "renameCollectionSameDB", "update" ]
  }],
  roles:[]}
);

db.createUser({
  user: 'admin',
  pwd: 'admin',
  roles: [
    {
      role: 'readWriteSystem',
      db: 'geo-mongo',
    },
  ],
});

db.grantRolesToUser(
    "admin",
    [
      { role: "readWrite", db: "geo-mongo" }
    ]
);
