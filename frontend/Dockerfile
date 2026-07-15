# ==========================================
# 阶段 1：Vite 构建前端
# ==========================================
FROM node:18-alpine AS build
WORKDIR /app
COPY . .
RUN npm ci --registry=https://registry.npmmirror.com
RUN npm run build

# ==========================================
# 阶段 2：Nginx 提供静态文件 + API 代理
# ==========================================
FROM nginx:alpine
# 用自定义配置覆盖默认 nginx.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
