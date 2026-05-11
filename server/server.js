const express = require("express");
const sqlite3 = require("sqlite3").verbose();

const app = express();
const port = 3000;

// Middleware
app.use(express.urlencoded({ extended: true }));
app.use(express.json());

// Database setup
const db = new sqlite3.Database("users.db");

// Create table if not exists
db.serialize(() => {
  console.log("Running creaete table");
  db.run(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      username TEXT,
      password TEXT,
      salt TEXT
    )
  `);
  console.log("After creaete table");

  // Insert a test user
  // password is password
  console.log("Running insert table");
  db.run(`
    INSERT OR IGNORE INTO  users (username, password, salt)
    VALUES ('admin', 'de4c58a4d8593482f30c68286327d39c4bf1feb7883cc3a3688e91c018e8c01a', '7/l5SRzaB0nQEiQGc8LEJw==')
  `);
  console.log("after insset table");
});

// Import routes
console.log("before user routes require");
const userRoutes = require("./routes/user");
console.log("after user routes require");
const updateRoutes = require("./routes/updates")

console.log("before user routes use");
// Pass db into routes
app.use("/user", (req, res, next) => {
  req.db = db;
  next();
}, userRoutes);
console.log("after user routes use");
app.use("/updates", (req, res, next) => {
  next();
}, updateRoutes);

// Basic route
app.get("/", (req, res) => {
  res.send(`Server running successfully at http://localhost:${port}!`);
});

// Start server
app.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
});