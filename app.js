const express = require("express");
const app = express();
const path = require("path");
const fs = require("fs"); // 引入文件系统模块，用于模拟持久化存储（实际上这里不会持久化到磁盘）

const host = "0.0.0.0"; // 监听所有接口
const port = 3000; // 监听的端口

let history = []; // 创建一个数组来存储历史记录

// 加载历史记录（这里实际上是从一个模拟的持久化存储中加载，实际上应该是从数据库或文件系统中加载）
function loadHistory() {
  // 假设我们从文件系统中加载历史记录，这里用硬编码的数据代替
  history = [{ timestamp: Date.now(), text: "started" }];
}

// 保存历史记录（这里实际上是将数据写入一个模拟的持久化存储，实际上应该是写入数据库或文件系统）
function saveHistory() {
  // 假设我们将历史记录保存到文件系统中，这里只是简单地打印出来
  console.log("Saving history:", history);
  // 在实际应用中，你会将history数组序列化并写入到文件或数据库中
}

// 中间件：验证JSON请求体中的字段不能为空，并限制字符长度
function validateFields(req, res, next) {
  const fieldsToValidate = ["text"]; // 根据需要添加或移除字段
  for (const field of fieldsToValidate) {
    if (!req.body.hasOwnProperty(field) || req.body[field].trim() === "") {
      return res
        .status(400)
        .json({ status: "failed", message: `${field} is required.` });
    }
    if (req.body[field].length > 8000) {
      return res
        .status(400)
        .json({ status: "failed", message: `${field} is too long.` });
    }
  }
  next();
}

// 初始化时加载历史记录
loadHistory();

app.use(express.static(path.join(__dirname, "public")));
app.use(express.json());

// 处理GET请求，显示文本编辑器
app.get("/", (req, res) => {
  res.sendFile(__dirname + "/public/editor.html");
});

// 处理POST请求，保存文本到内存缓存
app.post("/save", validateFields, (req, res) => {
  const newText = req.body.text; // 获取要保存的文本

  // 将新的历史记录项添加到数组中（这里简单使用当前时间戳作为标识）
  history.push({ timestamp: Date.now(), text: newText });

  // 保存历史记录到模拟的持久化存储中
  saveHistory();

  res.json({ status: "success", message: "saved successfully." });
});

app.get("/load", (req, res) => {
  res.json(history); // 将历史记录作为JSON返回
});

app.listen(port, host, () => {
  console.log(`running on port ${port}`);
});
