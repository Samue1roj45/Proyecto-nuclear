FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ARG GOOGLE_CLIENT_ID=""
ARG FACEBOOK_APP_ID=""
RUN if [ -n "$GOOGLE_CLIENT_ID" ]; then \
      sed -i "s|googleClientId: ''|googleClientId: '${GOOGLE_CLIENT_ID}'|g" src/environments/environment.production.ts; \
    fi && \
    if [ -n "$FACEBOOK_APP_ID" ]; then \
      sed -i "s|facebookAppId: ''|facebookAppId: '${FACEBOOK_APP_ID}'|g" src/environments/environment.production.ts; \
    fi

RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80
