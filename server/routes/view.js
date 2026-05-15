const express = require("express");
const path = require("path");
const router = express.Router();

router.get('/get_new_view', (req, res) => {
    const file = path.join(__dirname, '/../files/new_view.dex');
    res.download(file); 
});

module.exports = router;