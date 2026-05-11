const crypto = require("crypto");
const express = require("express");
const router = express.Router();


router.get("/get_update", (req, res) => {
    try {
        // Create a random string of 20 letters
        console.log("Received get_update");
        const challenge = crypto.randomBytes(10).toString('hex');
        return res.json({success: true, message: challenge})
    } catch (err) {
        return res.json({ success: false, error: err.message});
    }
});

router.post("/confirm_update", (req, res) => {
    try {
        console.log("Received confirm_update");
        return res.json({success: true, message: "Received check"})
    } catch (err) {
        return res.json({ success: false, error: err.message});
    } 
});

module.exports = router;