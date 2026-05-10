const express = require("express");
const router = express.Router();

/* Login function
 * Uses SQL with no checks, so it's open to SQL injection
 * Returns { success: false, error: err.message} if smth went wrong
 * Returns { success: true, message: "Login successful"} if login succesfull
 * Sends the flag if the correct admin password is submitted
*/
router.post("/login", (req, res) => {
    try {
        const db = req.db;
        const { username, password} = req.body;
        //console.log("Received a login request. Payload: \n {"+username+", "+password+"}\n");



        // Check for correct admin login, if so, we send the flag
        const admin_query = `
        SELECT * FROM users 
        WHERE username = 'admin'
        `; 

        db.get(admin_query, (err, row) => {
            if (err){
            return res.json({ success: false, error: err.message});
            }

            if (row && row.password == password) {
                return res.json({success: true, message: "Congrats: FLAG{adm1n_l0g1n_succ4s}"});
            } else {
                // Only do the normal check if they didnt enter the admin password
                const query = `
                SELECT * FROM users 
                WHERE username = '${username}' AND password = '${password}'
                `;

                db.get(query, (err, row) => {
                    if (err){
                    return res.json({ success: false, error: err.message});
                    }

                    if (row) {
                    res.json({
                        success: true,
                        message: "Login successful",
                    });
                    } else {
                        res.json({
                            success: false,
                            error: "Invalid credentials",
                        });
                    }
                });
            }
        });

    } catch (err) {
        return res.json({ success: false, error: err.message});
    }
});

/* Login function
 * Uses SQL with no checks, so it's open to SQL injection
 * Returns { success: false, error: err.message} if smth went wrong
 * Returns { success: true, message: "User added successfully"} if succesfull
*/
router.post("/register", (req, res) => {
    try{
        const db = req.db;
        const { username, password, salt} = req.body;

        //console.log("Received a register request. Payload: \n {"+username+", "+password+", "+salt+"}\n");

        // Check first if username already exists
        const query_check = `
        SELECT * FROM users 
        WHERE username = '${username}'
        `;

        db.get(query_check, (err, row) => {
            if (err){
            return res.json({ success: false, error: err.message});
            }

            if (row) {
            return res.json({
                success: false,
                error: "username exists already",
            });
            } else {
                // Add user to the db
                const query_add = `
                INSERT INTO users (username, password, salt)
                VALUES ('${username}', '${password}', '${salt}')
                `;

                db.run(query_add, (err) => {
                if (err){
                    return res.json({ success: false, error: err.message});
                }
                res.json({success: true, message: `User ${username} added succesfully`});
                });
            }
        });

        

    } catch (error){
        return res.json({ success: false, error: err.message});
    }
});


router.get("/get_salt", (req, res) => {
    try {
        const db = req.db;
        const {username} = req.query;
        //console.log("Received a get_salt request. Payload: \n" + username + "\n");

        const query = `
        SELECT * FROM users 
        WHERE username = '${username}'
        `;

        db.get(query, (err, row) => {
            if (err){
            return res.json({ success: false, error: err.message});
            }

            if (row) {
            res.json({
                success: true,
                message: row.salt,
            });
            } else {
                res.json({
                    success: false,
                    error: "Invalid user",
                });
            }
        });
    } catch (err) {
        return res.json({ success: false, error: err.message});
    }
});


module.exports = router;