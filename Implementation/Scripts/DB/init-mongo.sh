mongo -- "$MONGO_INITDB_DATABASE" <<EOF
db = db.getSiblingDB('$MONGO_DB_NAME_DOCKER');
db.createUser(
  {
    user: '$DB_USER',
    pwd: '$DB_PWD',
    roles: [{ role: 'readWrite', db: '$DB_NAME' }],
  },
);