# Postman 接口测试集合

成员4交付的 Postman 集合位于：

- `tests/postman/API接口测试集合.postman_collection.json`

使用方式：

1. 在 Postman 中导入该 JSON 文件。
2. 设置集合变量 `baseUrl`，默认是 `http://localhost:8080`。
3. 根据本地测试数据调整 `username`、`password`、`experimentId`。
4. 依次运行集合中的 8 个请求，覆盖小程序主流程接口。

集合覆盖登录、实验列表、实验详情、报名、我的报名、通知、提交申诉、我的申诉。
