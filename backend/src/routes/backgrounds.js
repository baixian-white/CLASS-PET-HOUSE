const router = require('express').Router();
const fs = require('fs');
const path = require('path');

const BG_DIR = path.join(__dirname, '../../../assets/背景图');
const ALLOWED_EXT = new Set(['.png', '.jpg', '.jpeg', '.webp', '.gif']);

function listBackgrounds() {
  if (!fs.existsSync(BG_DIR)) return [];
  const entries = fs.readdirSync(BG_DIR, { withFileTypes: true });
  return entries
    .filter((e) => e.isFile())
    .map((e) => e.name)
    .filter((name) => ALLOWED_EXT.has(path.extname(name).toLowerCase()));
}

router.get('/', (req, res) => {
  try {
    const files = listBackgrounds();
    res.json({ files });
  } catch (err) {
    res.status(500).json({ error: '获取背景图失败' });
  }
});

module.exports = router;
