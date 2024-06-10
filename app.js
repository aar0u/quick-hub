const express = require("express");
const app = express();
const path = require("path");

const host = "0.0.0.0"; // 监听所有接口
const port = 3000; // 监听的端口

// 使用全局变量作为内存缓存
let textCache = "";

app.use(express.static(path.join(__dirname, "public")));

// 处理GET请求，显示文本编辑器
app.get("/", (req, res) => {
  res.sendFile(__dirname + "/public/editor.html");
});

// 处理POST请求，保存文本到内存缓存
app.post("/save", express.urlencoded({ extended: true }), (req, res) => {
  textCache = req.body.text; // 将文本保存到缓存
  res.sendStatus(200); // 返回成功状态
});

// 处理GET请求，从内存缓存中读取文本并返回
app.get("/load", (req, res) => {
  res.send(textCache); // 返回缓存中的文本
});

app.listen(port, host, () => {
  console.log(`running on port ${port}`);
});
