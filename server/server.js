const express = require("express");
const sqlite3 = require("sqlite3").verbose();
const bodyParser = require("body-parser");

const app = express();
const port = 3000;

// Middleware
app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

// Database setup
const db = new sqlite3.Database("users.db");

// Create table if not exists
db.serialize(() => {
  db.run(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT,
      password TEXT,
      salt TEXT
    )
  `);

  // Insert a test user
  db.run(`
    INSERT INTO users (username, password, salt)
    VALUES ('admin', 'TzZGLbtLNduGQSFOB8taWIGYatGENQvxJgFDHumqXBE=', 'salt123')
  `);
});

// Import routes
const userRoutes = require("./routes/user");

// Pass db into routes
app.use("/user", (req, res, next) => {
  req.db = db;
  next();
}, userRoutes);

// Basic route
app.get("/", (req, res) => {
  res.send(`Server running successfully at http://localhost:${port}!`);
});

// Start server
app.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
});