FROM node:20.10.0-slim
WORKDIR /kiwi
COPY package* ./
RUN npm ci
COPY . .
CMD ["node", "index.js"]
