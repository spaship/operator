# REST API

The operator provides REST API. It's documented by Open API standard and available under
`http://<operator>/openapi` or actual copy is in [manifests/api](https://github.com/spaship/operator/tree/main/manifests/api) directory.

## API List

The `/api` returns actual list of APIs.

```json
[
  "/api/webhook",
  "/api/v1/website/search",
  "/api/v1/website/component/search?namespace={namespace}&website={website}&env={env}",
  "/api/v1/website/component/info?namespace={namespace}&website={website}&env={env}&name={component_name}"
]
```

## Webhook API

Under `/api/webhook` is exposed Webhook API to consume Git webhook. 

## Website API

Website API provides information about deployed websites.
Used by SPAship Manager UI.

 * /api/v1/website/component/info - Website component detail - get actual data via Content API